/*-
 * [LICENSE]
 * Taskboard
 * - - -
 * Copyright (C) 2015 - 2016 Objective Solutions
 * - - -
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
package objective.taskboard.followup.impl;

import static java.nio.file.Files.delete;

import objective.taskboard.followup.FollowUpTemplateValidator;
import objective.taskboard.followup.UpdateFollowUpService;
import objective.taskboard.utils.XmlUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.springframework.stereotype.Service;
import org.w3c.dom.NodeList;

import javax.xml.transform.TransformerException;

import java.io.*;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Service
public class DefaultUpdateFollowUpService implements UpdateFollowUpService {

    @Override
    public void validateTemplate(Path decompressed) throws FollowUpTemplateValidator.InvalidTemplateException {
        Path sheetXml = searchFromJiraSheet(decompressed);
        if(!isEmpty(XmlUtils.xpath(sheetXml.toFile(), "//sheetData/row[@r>1]/c/v/text()"))) {
            throw new FollowUpTemplateValidator.InvalidTemplateException();
        }
    }

    @Override
    public void updateFromJiraTemplate(Path decompressed, Path fromJiraTemplate) throws IOException {
        URL original = DefaultUpdateFollowUpService.class.getResource("/followup-template/sheet7-template-raw.xml");
        String newRowContent = getRowContent(decompressed);
        Map<String, Object> map = new HashMap<>();
        map.put("headerRow", newRowContent);
        String updatedXml = StrSubstitutor.replace(IOUtils.toString(original, "UTF-8"), map);
        FileUtils.write(fromJiraTemplate.toFile(), updatedXml, "UTF-8");
    }

    @Override
    public void updateSharedStringsInitial(Path decompressed, Path sharedStringsInitial) throws IOException {
        Path source = decompressed.resolve("xl/sharedStrings.xml");
        XmlUtils.format(source.toFile(), sharedStringsInitial.toFile());
    }

    @Override
    public void deleteGeneratedFiles(Path decompressed) throws IOException {
        delete(searchFromJiraSheet(decompressed));
        delete(decompressed.resolve("xl/sharedStrings.xml"));
    }

    // ---

    private String getRowContent(Path decompressed) {
        Path sheetXml = searchFromJiraSheet(decompressed);
        try {
            return XmlUtils.asString(XmlUtils.xpath(sheetXml.toFile(), "//sheetData/row[@r=1]"));
        } catch (TransformerException e) {
            throw new FollowUpTemplateValidator.InvalidTemplateException(e);
        }
    }

    private static boolean isEmpty(NodeList nodeList) {
        return nodeList.getLength() == 0;
    }

    private static Path searchFromJiraSheet(Path decompressed) {
        try {
            Path wbXml = decompressed.resolve("xl/workbook.xml");
            String relId = XmlUtils.asString(XmlUtils.xpath(wbXml.toFile(), "//sheet[@name='From Jira']/@id"));
            Path wbRelXml = decompressed.resolve("xl/_rels/workbook.xml.rels");
            String sheetId = XmlUtils.asString(XmlUtils.xpath(wbRelXml.toFile(), "//Relationship[@Id='" + relId + "']/@Target"));
            return decompressed.resolve("xl/" + sheetId);
        } catch (TransformerException e) {
            throw new FollowUpTemplateValidator.InvalidTemplateException(e);
        }
    }

}
