package objective.taskboard.google;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class SpreadsheetUtilsTest {

    @Test
    public void columnLetterToIndex() {
        assertEquals(0, SpreadsheetUtils.columnLetterToIndex("A"));
        assertEquals(1, SpreadsheetUtils.columnLetterToIndex("B"));
        assertEquals(2, SpreadsheetUtils.columnLetterToIndex("C"));

        assertEquals(26, SpreadsheetUtils.columnLetterToIndex("AA"));
        assertEquals(27, SpreadsheetUtils.columnLetterToIndex("AB"));
        
        assertEquals(702, SpreadsheetUtils.columnLetterToIndex("AAA"));
    }

    @Test
    public void columnIndexToLetter() {
        assertEquals("A", SpreadsheetUtils.columnIndexToLetter(0));
        assertEquals("Z", SpreadsheetUtils.columnIndexToLetter(25));
        assertEquals("AA", SpreadsheetUtils.columnIndexToLetter(26));
        assertEquals("AB", SpreadsheetUtils.columnIndexToLetter(27));
    }

    @Test
    public void columnLetterComparator() {
        List<String> letters = asList("Z", "AB", "A", "AZ", "R");
        letters.sort(SpreadsheetUtils.COLUMN_LETTER_COMPARATOR);
        
        assertEquals(asList("A", "R", "Z", "AB", "AZ"), letters);
    }
}
