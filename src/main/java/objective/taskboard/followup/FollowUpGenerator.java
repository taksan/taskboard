package objective.taskboard.followup;

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

import java.io.InputStream;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FollowUpGenerator {

    private static final String PROPERTY_XML_SPACE_PRESERVE = " xml:space=\"preserve\"";
    private static final String TAG_T_IN_SHARED_STRINGS = "t";
    private static final String PATH_SHARED_STRINGS_INITIAL = "followup-template/sharedStrings-initial.xml";
    private static final String PATH_SHARED_STRINGS_TEMPLATE = "followup-template/sharedStrings-template.xml";
    private static final String PATH_SI_SHARED_STRINGS_TEMPLATE = "followup-template/si-sharedStrings-template.xml";
    private static final String PATH_SHEET_7_TEMPLATE = "followup-template/sheet7-template.xml";
    private static final String PATH_ROW_SHEET_TEMPLATE = "followup-template/row-sheet-template.xml";

    @Autowired
    private FollowupDataProvider provider;

    public ByteArrayResource generate() {
        try {
            Map<String, Long> sharedStrings = getSharedStringsTemplate();
            generateJiraDataSheet(sharedStrings);
            generateSharedStrings(sharedStrings);
            // write (generateJiraDataSheet)
            // write (generateSharedStrings)
            // zip
            // read bytes

            return null;
        } catch (Exception e) {
            log.error(e.getMessage() == null ? e.toString() : e.getMessage());
            return null;
        }
    }

    Map<String, Long> getSharedStringsTemplate() throws ParserConfigurationException, SAXException, IOException {
        Map<String, Long> sharedStrings = new HashMap<String, Long>();
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream(PATH_SHARED_STRINGS_INITIAL);
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document doc = docBuilder.parse(inputStream);
        doc.getDocumentElement().normalize();
        NodeList nodes = doc.getElementsByTagName(TAG_T_IN_SHARED_STRINGS);

        for (Long index = 0L; index < nodes.getLength(); index++) {
            Node node = nodes.item(index.intValue());
            if (node.getNodeType() != Node.ELEMENT_NODE)
                continue;

            sharedStrings.put(node.getTextContent(), index);
        }
        return sharedStrings;
    }

    String generateJiraDataSheet(Map<String, Long> sharedStrings) throws IOException {
        String rowTemplate = getStringFromXML(PATH_ROW_SHEET_TEMPLATE);
        String rows = "";
        int rowNumber = 2;

        for (FollowUpData followUpData : provider.getJiraData()) {
            Map<String, Object> rowValues = new HashMap<String, Object>();
            rowValues.put("rowNumber", rowNumber);
            rowValues.put("project", getIndexInSharedStrings(sharedStrings, followUpData.project));
            rowValues.put("demandType", getIndexInSharedStrings(sharedStrings, followUpData.demandType));
            rowValues.put("demandStatus", getIndexInSharedStrings(sharedStrings, followUpData.demandStatus));
            rowValues.put("demandNum", getIndexInSharedStrings(sharedStrings, followUpData.demandNum));
            rowValues.put("demandSummary", getIndexInSharedStrings(sharedStrings, followUpData.demandSummary));
            rowValues.put("demandDescription", getIndexInSharedStrings(sharedStrings, followUpData.demandDescription));
            rowValues.put("taskType", getIndexInSharedStrings(sharedStrings, followUpData.taskType));
            rowValues.put("taskStatus", getIndexInSharedStrings(sharedStrings, followUpData.taskStatus));
            rowValues.put("taskNum", getIndexInSharedStrings(sharedStrings, followUpData.taskNum));
            rowValues.put("taskSummary", getIndexInSharedStrings(sharedStrings, followUpData.taskSummary));
            rowValues.put("taskDescription", getIndexInSharedStrings(sharedStrings, followUpData.taskDescription));
            rowValues.put("taskFullSescription", getIndexInSharedStrings(sharedStrings, followUpData.taskFullSescription));
            rowValues.put("subtaskType", getIndexInSharedStrings(sharedStrings, followUpData.subtaskType));
            rowValues.put("subtaskStatus", getIndexInSharedStrings(sharedStrings, followUpData.subtaskStatus));
            rowValues.put("subtaskNum", getIndexInSharedStrings(sharedStrings, followUpData.subtaskNum));
            rowValues.put("subtaskSummary", getIndexInSharedStrings(sharedStrings, followUpData.subtaskSummary));
            rowValues.put("subtaskDescription", getIndexInSharedStrings(sharedStrings, followUpData.subtaskDescription));
            rowValues.put("subtaskFullDescription", getIndexInSharedStrings(sharedStrings, followUpData.subtaskFullDescription));
            rowValues.put("demandId", getIndexInSharedStrings(sharedStrings, followUpData.demandId));
            rowValues.put("taskId", getIndexInSharedStrings(sharedStrings, followUpData.taskId.toString()));
            rowValues.put("subtaskId", getIndexInSharedStrings(sharedStrings, followUpData.subtaskId.toString()));
            rowValues.put("planningType", getIndexInSharedStrings(sharedStrings, followUpData.planningType));
            rowValues.put("taskRelease", getIndexInSharedStrings(sharedStrings, followUpData.taskRelease));
            rowValues.put("worklog", getIndexInSharedStrings(sharedStrings, followUpData.worklog.toString()));
            rowValues.put("wrongWorklog", getIndexInSharedStrings(sharedStrings, followUpData.wrongWorklog.toString()));
            rowValues.put("demandBallpark", getIndexInSharedStrings(sharedStrings, followUpData.demandBallpark.toString()));
            rowValues.put("taskBallpark", getIndexInSharedStrings(sharedStrings, followUpData.taskBallpark.toString()));
            rowValues.put("tshirtSize", getIndexInSharedStrings(sharedStrings, followUpData.tshirtSize));
            rowValues.put("queryType", getIndexInSharedStrings(sharedStrings, followUpData.queryType));
            rows += StrSubstitutor.replace(rowTemplate, rowValues);
            rowNumber++;
        }

        String sheetTemplate = getStringFromXML(PATH_SHEET_7_TEMPLATE);
        Map<String, String> sheetValues = new HashMap<String, String>();
        sheetValues.put("rows", rows);
        return StrSubstitutor.replace(sheetTemplate, sheetValues);
    }

    private String getStringFromXML(String pathXML) throws IOException {
        InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream(pathXML);
        return IOUtils.toString(inputStream, "UTF-8");
    }

    private Long getIndexInSharedStrings(Map<String, Long> sharedStrings, String followUpDataAttribute) {
        Long index = sharedStrings.get(followUpDataAttribute);
        if (index != null)
            return index;

        index = Long.valueOf(sharedStrings.size());
        sharedStrings.put(followUpDataAttribute, index);
        return index;
    }

    String generateSharedStrings(Map<String, Long> sharedStrings) throws IOException {
        String siSharedStringsTemplate = getStringFromXML(PATH_SI_SHARED_STRINGS_TEMPLATE);
        List<String> sharedStringsSorted = sharedStrings.keySet().stream()
            .sorted((s1, s2) -> sharedStrings.get(s1).compareTo(sharedStrings.get(s2)))
            .collect(toList());
        String allSharedStrings = "";

        for (String sharedString : sharedStringsSorted) {
            Map<String, Object> siValues = new HashMap<String, Object>();
            siValues.put("preserve", sharedString.endsWith(" ") ? PROPERTY_XML_SPACE_PRESERVE : "");
            siValues.put("sharedString", StringEscapeUtils.escapeXml(sharedString));
            allSharedStrings += StrSubstitutor.replace(siSharedStringsTemplate, siValues);
        }

        String sharedStringsTemplate = getStringFromXML(PATH_SHARED_STRINGS_TEMPLATE);
        Map<String, Object> sharedStringsValues = new HashMap<String, Object>();
        sharedStringsValues.put("sharedStringsSize", sharedStringsSorted.size());
        sharedStringsValues.put("allSharedStrings", allSharedStrings);
        return StrSubstitutor.replace(sharedStringsTemplate, sharedStringsValues);
    }
}
