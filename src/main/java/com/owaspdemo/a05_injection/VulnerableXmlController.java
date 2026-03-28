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
 * VULNERABLE: Parses XML with default DocumentBuilderFactory settings.
 * External entities and DTDs are enabled, allowing file exfiltration.
 *
 * Try this payload:
 * <?xml version="1.0"?>
 * <!DOCTYPE foo [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
 * <products><product><name>&xxe;</name></product></products>
 */
@RestController
@RequestMapping("/api/v1/vulnerable/products")
public class VulnerableXmlController {

    @PostMapping(value = "/import", consumes = MediaType.APPLICATION_XML_VALUE)
    public List<String> importProducts(@RequestBody String xml) throws Exception {
        // BAD: Default DocumentBuilderFactory allows external entities
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

        List<String> names = new ArrayList<>();
        NodeList nodes = doc.getElementsByTagName("name");
        for (int i = 0; i < nodes.getLength(); i++) {
            names.add(nodes.item(i).getTextContent());
        }
        return names;
    }
}
