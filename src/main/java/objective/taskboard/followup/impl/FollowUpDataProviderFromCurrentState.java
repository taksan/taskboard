package objective.taskboard.followup.impl;

/*-
 * [LICENSE]
 * Taskboard
 * ---
 * Copyright (C) 2015 - 2017 Objective Solutions
 * ---
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * [/LICENSE]
 */

import static org.apache.commons.lang.ObjectUtils.defaultIfNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import objective.taskboard.data.CustomField;
import objective.taskboard.data.Issue;
import objective.taskboard.followup.FollowUpData;
import objective.taskboard.followup.FollowupDataProvider;
import objective.taskboard.issueBuffer.AllIssuesBufferService;
import objective.taskboard.issueBuffer.IssueBufferState;
import objective.taskboard.jira.JiraProperties;
import objective.taskboard.jira.JiraProperties.BallparkMapping;
import objective.taskboard.jira.MetadataService;

@Service
public class FollowUpDataProviderFromCurrentState implements FollowupDataProvider {
    @Autowired
    private JiraProperties jiraProperties;
    
    @Autowired
    private MetadataService metadataService;
    
    @Autowired
    private AllIssuesBufferService issueBufferService;
    
    private Map<String, Issue> demandsByKey;
    private Map<String, Issue> featuresByKey;
    private Map<String, FollowUpData> followUpBallparks;
    
    @Override
    public IssueBufferState getFollowupState() {
        return issueBufferService.getState();
    }
    
    @Override
    public List<FollowUpData> getJiraData(String[] includeProjects) {
        List<String> i = Arrays.asList(includeProjects);
        List<Issue> issuesVisibleToUser = issueBufferService.getAllIssues().stream()
	         .filter(issue -> isAllowedStatus(issue.getStatus()))
	         .filter(issue -> i.contains(issue.getProjectKey()))
	         .collect(Collectors.toList());

        LinkedList<Issue> issues = new LinkedList<>(issuesVisibleToUser);
        
        followUpBallparks = new LinkedHashMap<String, FollowUpData>();
        
        demandsByKey = makeDemandBallparks(issues);
        
        featuresByKey = makeFeatureBallparks(issues);
        
        final List<FollowUpData> result = makeSubtasks(issues);

        result.addAll(followUpBallparks.values());
        
        result.sort(new Comparator<FollowUpData>() {
            public int compare(FollowUpData o1, FollowUpData o2) {
                String o1Order = o1.demandOrder + o1.taskOrder + o1.subtaskOrder;
                String o2Order = o2.demandOrder + o2.taskOrder + o2.subtaskOrder;
                return (o1Order).compareTo(o2Order);
            }
        });
        
        return result;
    }
    
    private boolean isAllowedStatus(long status) {
        return !jiraProperties.getFollowup().getStatusExcludedFromFollowup().contains(status);
    }

    private Map<String, Issue> makeDemandBallparks(LinkedList<Issue> issues) {
        Map<String, Issue> demands = new LinkedHashMap<>();
        Iterator<Issue> it = issues.iterator();
        while(it.hasNext()) {
            Issue issue = it.next();
            if (issue.isDemand()) {
                followUpBallparks.put(issue.getIssueKey(), createBallparkDemand(issue));
                demands.put(issue.getIssueKey(), issue);
                it.remove();
            }
        }
        return demands;
    }
    
    private Map<String, Issue> makeFeatureBallparks(LinkedList<Issue> issues) {
        Map<String, Issue> features = new LinkedHashMap<String, Issue>();
        
        Iterator<Issue> it = issues.iterator();
        while(it.hasNext()) {
            Issue feature = it.next();
            if (!feature.isFeature()) 
                continue;
            it.remove();
            
            Issue demand = demandsByKey.get(feature.getParent());
            if (demand != null)
                followUpBallparks.remove(demand.getIssueKey());
            
            features.put(feature.getIssueKey(), feature);
            
            if (jiraProperties.getFollowup().getFeatureStatusThatDontGenerateBallpark().contains(feature.getStatus()))
                continue;
            
            List<BallparkMapping> mappingList = getBallparksOrCry(feature);

            for (BallparkMapping mapping : mappingList) {
                String issueKeyAndTShirtSize = feature.getIssueKey()+mapping.getTshirtCustomFieldId();
                followUpBallparks.put( issueKeyAndTShirtSize, createBallparkFeature(demand, feature, mapping));
            }
        }
        return features;
    }

    private List<FollowUpData> makeSubtasks(LinkedList<Issue> issues) {
        
        final List<FollowUpData> subtasksFollowups = new LinkedList<FollowUpData>();
        Iterator<Issue> it = issues.iterator();
        while(it.hasNext()) {
            Issue issue = it.next();
            Issue feature = featuresByKey.get(issue.getParent());
            if (feature == null)
                continue;
            
            Issue demand  = demandsByKey.get(feature.getParent());
            if (demand != null)
                followUpBallparks.remove(demand.getIssueKey());
            
            subtasksFollowups.add(createSubTaskFollowup(demand, feature, issue));

            if (jiraProperties.getFollowup().getSubtaskStatusThatDontPreventBallparkGeneration().contains(issue.getStatus()))
                continue;
            
            String featureTshirtForThisSubTask = "";
            List<BallparkMapping> mappingList = getBallparksOrCry(feature);
            for (BallparkMapping mapping : mappingList) {
                if (mapping.getJiraIssueTypes().contains(issue.getType())) 
                    featureTshirtForThisSubTask = mapping.getTshirtCustomFieldId();
            }
            
            followUpBallparks.remove(feature.getIssueKey()+featureTshirtForThisSubTask);
        }
        return subtasksFollowups;
    }
    
    private List<BallparkMapping> getBallparksOrCry(Issue issue) {
        List<BallparkMapping> mappings = issue.getActiveBallparkMappings();
        if (mappings == null) {
            throw new IllegalStateException(
                    "Ballpark mapping for issue type '"+issue.getIssueTypeName()+"' (id "+issue.getType()+") missing in configuration");
        }
        return mappings;
    }

    private FollowUpData createBallparkDemand(Issue demand) {
        String demandPriorityOrder = demand.getPriorityOrder() != null ? demand.getPriorityOrder().toString() : "";
        FollowUpData followUpData = new FollowUpData();
        followUpData.planningType = "Ballpark";
        followUpData.project = demand.getProject();
        
        followUpData.demandId = demand.getId();
        followUpData.demandType = demand.getIssueTypeName();
        followUpData.demandStatus= demand.getStatusOrderedName();
        followUpData.demandNum = demand.getIssueKey();
        followUpData.demandSummary = demand.getSummary();
        followUpData.demandDescription = issueDescription("M", demand);
        followUpData.demandOrder = demand.getStatusPriority() + demandPriorityOrder;
        
        followUpData.taskType = "BALLPARK - Demand";
        followUpData.taskStatus = getBallparkStatus();
        followUpData.taskId = 0L;
        followUpData.taskNum = followUpData.demandNum;
        followUpData.taskSummary="Dummy Feature";
        followUpData.taskDescription = issueDescription(0, demand.getSummary());
        followUpData.taskFullDescription = issueFullDescription("BALLPARK - Demand", "M", 0, demand.getSummary());
        followUpData.taskRelease = (String) defaultIfNull(getRelease(demand), "No release set");
        
        followUpData.subtaskType = "BALLPARK - Demand";
        followUpData.subtaskStatus = followUpData.demandStatus;
        followUpData.subtaskId = 0L;
        followUpData.subtaskNum = demand.getProjectKey()+"-0";
        followUpData.subtaskSummary = followUpData.demandSummary;
        followUpData.subtaskDescription = issueDescription("M", 0, demand.getSummary());
        followUpData.subtaskFullDescription = issueFullDescription("BALLPARK - Demand", "M", 0, demand.getSummary());
        followUpData.tshirtSize = "M";
        followUpData.worklog = 0.0;
        followUpData.wrongWorklog = timeSpentInHour(demand); 
        followUpData.demandBallpark = originalEstimateInHour(demand);
        followUpData.taskBallpark = 0.0;
        followUpData.queryType = "DEMAND BALLPARK";
        return followUpData;
    }
    
    private Double timeSpentInHour(Issue issue) {
        if (issue.getTimeTracking() == null)
            return 0.0;
        if (issue.getTimeTracking().getTimeSpentMinutes() == null)
            return 0.0;
        return issue.getTimeTracking().getTimeSpentMinutes()/60.0;
    }
    
    private Double originalEstimateInHour(Issue issue) {
        if (issue == null)
            return 0.0;
        if (issue.getTimeTracking() == null)
            return 0.0;
        if (issue.getTimeTracking().getOriginalEstimateMinutes() == null)
            return 0.0;
        return issue.getTimeTracking().getOriginalEstimateMinutes()/60.0;
    }
    
    private FollowUpData createBallparkFeature(Issue demand, Issue task, BallparkMapping ballparkMapping) {
        FollowUpData followUpData = new FollowUpData();
        followUpData.planningType = "Ballpark";
        followUpData.project = task.getProject();
        
        if (demand != null) {
            String demandPriorityOrder = demand.getPriorityOrder() != null ? demand.getPriorityOrder().toString() : "";
            followUpData.demandType = demand.getIssueTypeName();
            followUpData.demandStatus= demand.getStatusOrderedName();
            followUpData.demandId = demand.getId();
            followUpData.demandNum = demand.getIssueKey();
            followUpData.demandSummary = demand.getSummary();
            followUpData.demandDescription = issueDescription("", demand);
            followUpData.demandOrder = demand.getStatusPriority() + demandPriorityOrder;
        }
        
        followUpData.taskType = task.getIssueTypeName();
        followUpData.taskStatus = task.getStatusOrderedName();
        followUpData.taskId = task.getId();
        followUpData.taskNum = task.getIssueKey();
        followUpData.taskSummary=task.getSummary();
        followUpData.taskDescription = issueDescription(task);
        followUpData.taskFullDescription = issueFullDescription(task);
        followUpData.taskRelease = coalesce(getRelease(task), getRelease(demand) ,"No release set");
        
        followUpData.subtaskType = ballparkMapping.getIssueType();
        followUpData.subtaskStatus = getBallparkStatus();
        followUpData.subtaskId = 0L;
        followUpData.subtaskNum = task.getProjectKey()+"-0";
        followUpData.subtaskSummary = ballparkMapping.getIssueType();
        followUpData.subtaskDescription = issueDescription(0, task.getSummary());
        followUpData.subtaskFullDescription = issueFullDescription(ballparkMapping.getIssueType(), "", 0, task.getSummary());
        followUpData.tshirtSize = task.getTshirtSizeOfSubtaskForBallpark(ballparkMapping);
        followUpData.worklog = 0.0;
        followUpData.wrongWorklog = timeSpentInHour(task); 
        followUpData.demandBallpark = originalEstimateInHour(demand);
        followUpData.taskBallpark = originalEstimateInHour(task);
        followUpData.queryType = "FEATURE BALLPARK";
        return followUpData;
    }
    
    private String getBallparkStatus() {
        return metadataService.getStatusById(jiraProperties.getFollowup().getBallparkDefaultStatus()).getName();
    }

    private FollowUpData createSubTaskFollowup(Issue demand, Issue task, Issue subtask) {
        String taskPriorityOrder = task.getPriorityOrder() != null ? task.getPriorityOrder().toString() : "";
        String subtaskPriorityOrder = subtask.getPriorityOrder() != null ? subtask.getPriorityOrder().toString() : "";
        FollowUpData followUpData = new FollowUpData();
        followUpData.planningType = "Plan";
        followUpData.project = task.getProject();
        
        if (demand != null) {
            String demandPriorityOrder = demand.getPriorityOrder() != null ? demand.getPriorityOrder().toString() : "";
            followUpData.demandId = demand.getId();
            followUpData.demandType = demand.getIssueTypeName();
            followUpData.demandStatus= demand.getStatusOrderedName();
            followUpData.demandNum = demand.getIssueKey();
            followUpData.demandSummary = demand.getSummary();
            followUpData.demandDescription = issueDescription("",demand);
            followUpData.demandBallpark = originalEstimateInHour(demand);
            followUpData.demandOrder = demand.getStatusPriority() + demandPriorityOrder;
        }
        
        followUpData.taskType = task.getIssueTypeName();
        followUpData.taskStatus = task.getStatusOrderedName();
        followUpData.taskId = task.getId();
        followUpData.taskNum = task.getIssueKey();
        followUpData.taskSummary=task.getSummary();
        followUpData.taskDescription = issueDescription(task);
        followUpData.taskFullDescription = issueFullDescription(task);
        followUpData.taskRelease = coalesce(getRelease(subtask), getRelease(task), getRelease(demand), "No release set");
        followUpData.taskOrder = task.getStatusPriority() + taskPriorityOrder;
        
        followUpData.subtaskType = subtask.getIssueTypeName();
        followUpData.subtaskStatus = subtask.getStatusOrderedName();
        followUpData.subtaskId = subtask.getId();
        followUpData.subtaskNum = subtask.getIssueKey();
        followUpData.subtaskSummary = subtask.getSummary();
        followUpData.subtaskOrder = task.getStatusPriority() + subtaskPriorityOrder;
        
        followUpData.subtaskDescription = issueDescription(subtask);
        followUpData.subtaskFullDescription = issueFullDescription(subtask);
        followUpData.tshirtSize = subtask.getTShirtSize();
        followUpData.worklog = timeSpentInHour(subtask);
        followUpData.wrongWorklog = 0.0; 
        followUpData.taskBallpark = originalEstimateInHour(task);
        followUpData.queryType = "SUBTASK PLAN";
        return followUpData;
    }

    private String getTshirtSize(Issue i) {
        if (i.isFeature()) return "";
        if (i.isDemand()) return "M";
        return i.getTShirtSize();
    }
    
    private String getRelease(Issue i) {
        if (i == null)
            return null;
        
        CustomField customField = (CustomField)i.getCustomFields().get(jiraProperties.getCustomfield().getRelease().getId());
        if (customField == null) return null;
        if (customField.getValue() == null)
            return null;
        return customField.getValue().toString();
    }

    private String issueDescription(Issue issue) {
        return issueDescription(getTshirtSize(issue), issue.getIssueKeyNum(), issue.getSummary());
    }
    
    private String issueDescription(String size, Issue issue) {
        return issueDescription(size, issue.getIssueKeyNum(), issue.getSummary());
    }
    
    private String issueDescription(Integer issueNum, String description) {
        return issueDescription(null, issueNum, description);
    }

    private String issueDescription(String size, Integer issueNum, String description) {
        String sizePart = StringUtils.isEmpty(size)?"":size+" | ";
        return String.format("%s%05d - %s", sizePart, issueNum, description);
    }
    
    private String issueFullDescription(Issue issue) {
        return issue.getStatusName() + " > " + issue.getIssueTypeName() +
                " | " + issueDescription(getTshirtSize(issue), issue.getIssueKeyNum(), issue.getSummary());
    }
    
    private String issueFullDescription(String issueType, String size, Integer issueNum, String description) {
        return issueType + " | " +issueDescription(size, issueNum, description);
    }
    
    private static <T> T coalesce(@SuppressWarnings("unchecked") T ...items) {
        for(T i : items) if(i != null) return i;
        return null;
    }
}
