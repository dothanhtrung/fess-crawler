/*
 * Copyright 2004-2009 the Seasar Foundation and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.seasar.robot.transformer.impl;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerException;

import org.cyberneko.html.parsers.DOMParser;
import org.seasar.framework.beans.util.Beans;
import org.seasar.framework.util.StringUtil;
import org.seasar.robot.Constants;
import org.seasar.robot.RobotCrawlAccessException;
import org.seasar.robot.RobotSystemException;
import org.seasar.robot.entity.AccessResultData;
import org.seasar.robot.entity.ResponseData;
import org.seasar.robot.entity.ResultData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XpathTransformer stores WEB data as XML content.
 * 
 * @author shinsuke
 * 
 */
public class XpathTransformer extends HtmlTransformer {
    private static final Logger logger = LoggerFactory // NOPMD
        .getLogger(XpathTransformer.class);

    private static final Pattern SPACE_PATTERN = Pattern.compile(
        "\\s+",
        Pattern.MULTILINE);

    protected Map<String, String> fieldRuleMap =
        new LinkedHashMap<String, String>();

    /** a flag to trim a space characters. */
    protected boolean trimSpace = true;

    protected String charsetName = Constants.UTF_8;

    /**
     * Class type returned by getData() method. The default is null(XML content
     * of String).
     */
    protected Class<?> dataClass = null;

    @Override
    protected void storeData(final ResponseData responseData,
            final ResultData resultData) {
        final DOMParser parser = getDomParser();
        try {
            final InputSource is =
                new InputSource(responseData.getResponseBody());
            if (responseData.getCharSet() != null) {
                is.setEncoding(responseData.getCharSet());
            }
            parser.parse(is);
        } catch (Exception e) {
            throw new RobotCrawlAccessException("Could not parse "
                + responseData.getUrl(), e);
        }
        final Document document = parser.getDocument();

        final StringBuilder buf = new StringBuilder(1000);
        buf.append(getResultDataHeader());
        for (Map.Entry<String, String> entry : fieldRuleMap.entrySet()) {
            final StringBuilder nodeBuf = new StringBuilder(255);
            try {
                final NodeList nodeList =
                    getXPathAPI().selectNodeList(document, entry.getValue());
                for (int i = 0; i < nodeList.getLength(); i++) {
                    final Node node = nodeList.item(i);
                    nodeBuf.append(node.getTextContent()).append(' ');
                }
            } catch (TransformerException e) {
                logger.warn("Could not parse a value of " + entry.getKey()
                    + ":" + entry.getValue());
            }
            buf.append(getResultDataBody(entry.getKey(), nodeBuf
                .toString()
                .trim()));
        }
        buf.append(getAdditionalData(responseData, document));
        buf.append(getResultDataFooter());

        try {
            resultData.setData(buf.toString().getBytes(charsetName));
        } catch (UnsupportedEncodingException e) {
            if (logger.isInfoEnabled()) {
                logger.info("Invalid charsetName: " + charsetName
                    + ". Changed to " + Constants.UTF_8, e);
            }
            charsetName = Constants.UTF_8;
            try {
                resultData.setData(buf.toString().getBytes(charsetName));
            } catch (UnsupportedEncodingException e1) {
                throw new RobotSystemException("Unexpected exception", e);
            }
        }
        resultData.setEncoding(charsetName);
    }

    protected String getResultDataHeader() {
        // TODO support other type
        return "<?xml version=\"1.0\"?>\n<doc>\n";
    }

    protected String getResultDataBody(final String name, final String value) {
        // TODO support other type
        // TODO trim(default)
        return "<field name=\"" + escapeXml(name) + "\">"
            + trimSpace(escapeXml(value != null ? value : "")) + "</field>\n";
    }

    protected String getResultDataBody(final String name,
            final List<String> values) {
        final StringBuilder buf = new StringBuilder();
        buf.append("<list>");
        if (values != null && !values.isEmpty()) {
            for (String value : values) {
                buf.append("<item>");
                buf.append(trimSpace(escapeXml(value)));
                buf.append("</item>");
            }
        }
        buf.append("</list>");
        // TODO support other type
        // TODO trim(default)
        return "<field name=\"" + escapeXml(name) + "\">" + buf.toString()
            + "</field>\n";
    }

    protected String getAdditionalData(final ResponseData responseData,
            final Document document) {
        return "";
    }

    protected String getResultDataFooter() {
        // TODO support other type
        return "</doc>";
    }

    protected String escapeXml(final String value) {
        // return StringEscapeUtils.escapeXml(value);
        return stripInvalidXMLCharacters(//
        value//
            .replaceAll("&", "&amp;")
            //
            .replaceAll("<", "&lt;")
            //
            .replaceAll(">", "&gt;")
            //
            .replaceAll("\"", "&quot;")
            //
            .replaceAll("\'", "&apos;")//
        );
    }

    private String stripInvalidXMLCharacters(final String in) {
        if (StringUtil.isEmpty(in)) {
            return in;
        }

        final StringBuilder buf = new StringBuilder();
        char c;
        for (int i = 0; i < in.length(); i++) {
            c = in.charAt(i);
            if ((c == 0x9) || (c == 0xA) || (c == 0xD)
                || ((c >= 0x20) && (c <= 0xD7FF))
                || ((c >= 0xE000) && (c <= 0xFFFD))
                || ((c >= 0x10000) && (c <= 0x10FFFF))) {
                buf.append(c);
            }
        }
        return buf.toString();
    }

    protected String trimSpace(final String value) {
        if (trimSpace) {
            final Matcher matcher = SPACE_PATTERN.matcher(value);
            return matcher.replaceAll(" ").trim();
        }
        return value;
    }

    public void addFieldRule(final String name, final String xpath) {
        fieldRuleMap.put(name, xpath);
    }

    /**
     * Returns data as XML content of String.
     * 
     * @return XML content of String.
     */
    @Override
    public Object getData(final AccessResultData accessResultData) {
        if (dataClass == null) {
            return super.getData(accessResultData);
        }

        final Map<String, Object> dataMap = getDataMap(accessResultData);
        if (Map.class.equals(dataClass)) {
            return dataMap;
        }

        try {
            final Object obj = dataClass.newInstance();
            Beans.copy(dataMap, obj).execute();
            return obj;
        } catch (Exception e) {
            throw new RobotSystemException(
                "Could not create/copy a data map to " + dataClass,
                e);
        }
    }

    protected Map<String, Object> getDataMap(
            final AccessResultData accessResultData) {
        // create input source
        final InputSource is =
            new InputSource(
                new ByteArrayInputStream(accessResultData.getData()));
        if (StringUtil.isNotBlank(accessResultData.getEncoding())) {
            is.setEncoding(accessResultData.getEncoding());
        }

        // create handler
        final DocHandler handler = new DocHandler();

        // create a sax instance
        final SAXParserFactory spfactory = SAXParserFactory.newInstance();
        try {
            // create a sax parser
            final SAXParser parser = spfactory.newSAXParser();
            // parse a content
            parser.parse(is, handler);

            return handler.getDataMap();
        } catch (Exception e) {
            throw new RobotSystemException(
                "Could not create a data map from XML content.",
                e);
        }
    }

    protected static class DocHandler extends DefaultHandler {
        private Map<String, Object> dataMap = new HashMap<String, Object>();

        private String fieldName;

        private boolean listData = false;

        private boolean itemData = false;

        public void startDocument() {
            dataMap.clear();
        }

        public void startElement(final String uri, final String localName,
                final String qName, final Attributes attributes) {
            if ("field".equals(qName)) {
                fieldName = attributes.getValue("name");
            } else if ("list".equals(qName)) {
                listData = true;
                if (!dataMap.containsKey(fieldName)) {
                    dataMap.put(fieldName, new ArrayList<String>());
                }
            } else if ("item".equals(qName)) {
                itemData = true;
            }
        }

        public void characters(final char[] ch, final int offset,
                final int length) {
            if (fieldName != null) {
                final Object value = dataMap.get(fieldName);
                if (listData && itemData) {
                    if (value != null) {
                        ((List<String>) value).add(new String(
                            ch,
                            offset,
                            length));
                    }
                } else {
                    if (value == null) {
                        dataMap.put(fieldName, new String(ch, offset, length));
                    } else {
                        dataMap.put(fieldName, value
                            + new String(ch, offset, length));
                    }
                }
            }
        }

        public void endElement(final String uri, final String localName,
                final String qName) {
            if ("field".equals(qName)) {
                fieldName = null;
            } else if ("list".equals(qName)) {
                listData = false;
            } else if ("item".equals(qName)) {
                itemData = false;
            }
        }

        public void endDocument() {
            // nothing
        }

        public Map<String, Object> getDataMap() {
            return dataMap;
        }
    }

    public Map<String, String> getFieldRuleMap() {
        return fieldRuleMap;
    }

    public void setFieldRuleMap(final Map<String, String> fieldRuleMap) {
        this.fieldRuleMap = fieldRuleMap;
    }

    public boolean isTrimSpace() {
        return trimSpace;
    }

    public void setTrimSpace(final boolean trimSpace) {
        this.trimSpace = trimSpace;
    }

    public String getCharsetName() {
        return charsetName;
    }

    public void setCharsetName(final String charsetName) {
        this.charsetName = charsetName;
    }

    public Class<?> getDataClass() {
        return dataClass;
    }

    public void setDataClass(final Class<?> dataClass) {
        this.dataClass = dataClass;
    }
}
