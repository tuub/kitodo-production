package org.kitodo.production.plugin.step.archive;

import java.util.ArrayList;
import java.util.List;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class MetsErrorHandler implements ErrorHandler {

    private List<String> messages = new ArrayList<>();

    public List<String> getMessages() {
        return messages;
    }

    @Override
    public void warning(SAXParseException e) throws SAXException {
        messages.add(e.toString());
    }

    @Override
    public void error(SAXParseException e) throws SAXException {
        messages.add(e.toString());
    }

    @Override
    public void fatalError(SAXParseException e) throws SAXException {
        messages.add(e.toString());
    }
}
