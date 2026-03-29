package com.owaspdemo.a05_injection;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vulnerable/products")
@Tag(name = "A05 - Injection")
public class VulnerableXmlController {

    @PostMapping(value = "/import", consumes = MediaType.APPLICATION_XML_VALUE)
    @Operation(summary = "Import products via XML (XXE vulnerable)", description = "Default DocumentBuilderFactory allows external entities")
    public List<String> importProducts(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(mediaType = "application/xml",
                            examples = @ExampleObject(value = "<?xml version=\"1.0\"?>\n<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>\n<products><product><name>&xxe;</name></product></products>")))
            @RequestBody String xml) throws Exception {
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
