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
package objective.taskboard.it;

import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.openqa.selenium.JavascriptExecutor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public abstract class AbstractUIWithCoverageIntegrationTest extends AbstractUIIntegrationTest {

    @Rule
    public final TestName testName = new TestName();

    @After
    public void saveCoverage() {
        String coverageReport = (String) ((JavascriptExecutor)webDriver).executeScript("return JSON.stringify(window.__coverage__);");
        File f = new File("target/istanbul-reports/" + testName.getMethodName() + ".json");
        try {
            if(f.getParentFile() != null)
                f.getParentFile().mkdirs();
            f.createNewFile();
            IOUtils.write(coverageReport, new FileOutputStream(f), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
