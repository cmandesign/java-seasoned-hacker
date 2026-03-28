package com.owaspdemo.a05_injection;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A05:2025 - Injection (XXE variant)
 *
 * SECURE: Disables DTDs, external entities, and external parameter entities.
 * Same XML payload with XXE will be rejected with a parse error.
 */
@RestController
@RequestMapping("/api/v1/secure/products")
public class SecureXmlController {

    @PostMapping(value = "/import", consumes = MediaType.APPLICATION_XML_VALUE)
    public List<String> importProducts(@RequestBody String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // GOOD: Disable all dangerous XML features
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

        List<String> names = new ArrayList<>();
        NodeList nodes = doc.getElementsByTagName("name");
        for (int i = 0; i < nodes.getLength(); i++) {
            names.add(nodes.item(i).getTextContent());
        }
        return names;
    }
}
