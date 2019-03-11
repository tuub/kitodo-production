package org.kitodo.production.plugin.step.archive;

import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class MetsErrorHandlerTest {

    private MetsErrorHandler errorHandler = new MetsErrorHandler();
    private SAXParseException saxErrorException = new SAXParseException("error", "a", "b", 1, 1);
    private SAXParseException saxFatalErrorException = new SAXParseException("fatal error", "a", "b", 1, 1);
    private SAXParseException saxWarningException = new SAXParseException("warning", "a", "b", 1, 1);

    @Test
    public void messages() throws SAXException {
        errorHandler.error(saxErrorException);
        errorHandler.fatalError(saxFatalErrorException);
        errorHandler.warning(saxWarningException);

        List<String> messages = errorHandler.getMessages();
        assertNotNull(messages);
        assertEquals(3, messages.size());
        assertTrue(messages.get(0).toLowerCase().contains("error"));
        assertTrue(messages.get(1).toLowerCase().contains("fatal error"));
        assertTrue(messages.get(2).toLowerCase().contains("warning"));
    }

}
