/*******************************************************************************
 * Permission to use and distribute this software is granted under the BSD 2-Clause
 * "Simplified" License (see http://opensource.org/licenses/BSD-2-Clause).
 * Copyright (c) 2016-2026 Wilhelm Burger. All rights reserved.
 * Visit https://imagingbook.com for additional details.
 ******************************************************************************/
package imagingbook.lensfun;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * We will use Files.list to find all .xml files and a single DocumentBuilder to parse them.
 * To prevent the "DTD not found" or "Internet connection" errors we discussed earlier, we will
 * explicitly tell the parser to look for your local lensfun-database.dtd.
 */
public class LensDatabaseManager {
    private final List<Lens> masterLensList = new ArrayList<>();
    private final Path localDbPath = Path.of(LensfunManager.LOCAL_LENSFUN_DB_PATH);

    public void loadAllLenses() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // Disable DTD validation so it doesn't try to go online
        factory.setValidating(false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        DocumentBuilder builder = factory.newDocumentBuilder();

        try (var paths = Files.list(localDbPath)) {
            paths.filter(p -> p.toString().endsWith(".xml"))
                    .forEach(path -> {
                        try {
                            parseFile(builder, path.toFile());
                        } catch (Exception e) {
                            System.err.println("Error parsing " + path + ": " + e.getMessage());
                        }
                    });
        }
        System.out.println("Loaded " + masterLensList.size() + " lenses into memory.");
    }

    private void parseFile(DocumentBuilder builder, File file) throws Exception {
        Document doc = builder.parse(file);
        doc.getDocumentElement().normalize();

        // Get all <lens> tags in this file
        NodeList nList = doc.getElementsByTagName("lens");

        for (int i = 0; i < nList.getLength(); i++) {
            Node node = nList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;

                Lens lens = new Lens();
                lens.setMaker(getTagValue("maker", element));
                lens.setModel(getTagValue("model", element));
                lens.setType(getTagValue("type", element));
                lens.setCropFactor(getTagValue("cropfactor", element));

                // --- retrieve all mounts ---
                NodeList mountList = element.getElementsByTagName("mount");
                for (int j = 0; j < mountList.getLength(); j++) {
                    String mountName = mountList.item(j).getTextContent();
                    lens.addMount(mountName);
                }

                parseDistortion(element, lens);
                parseTca(element, lens);
                parseVignetting(element, lens);

                masterLensList.add(lens);
            }
        }
    }

    private String getTagValue(String tagName, Element element) {
        NodeList list = element.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent();
        }
        return "Unknown";
    }

    private void parseDistortion(Element calibElement, Lens lens) {
        NodeList distortionList = calibElement.getElementsByTagName("distortion");

        for (int k = 0; k < distortionList.getLength(); k++) {
            Element distElem = (Element) distortionList.item(k);

            String model = distElem.getAttribute("model");
            double focal = getAttrDouble(distElem, "focal");

            // Pull all possible coefficients; non-existent ones return 0.0
            double k1 = getAttrDouble(distElem, "k1");
            double k2 = getAttrDouble(distElem, "k2");
            double a  = getAttrDouble(distElem, "a");
            double b  = getAttrDouble(distElem, "b");
            double c  = getAttrDouble(distElem, "c");

            lens.addDistortion(new Lens.Distortion(focal, model, k1, k2, a, b, c));
        }
    }

    // TODO: TCA not correct yet!!!
    // <tca model="poly3" focal="18" br="-0.0000385" vr="1.0001447" bb="0.0000468" vb="0.9999362"/>
    private void parseTca(Element calibElement, Lens lens) {
        NodeList tcaList = calibElement.getElementsByTagName("tca");
        for (int i = 0; i < tcaList.getLength(); i++) {
            Element el = (Element) tcaList.item(i);
            lens.addTca(new Lens.Tca(
                    getAttrDouble(el, "focal"),
                    el.getAttribute("model"),
                    getAttrDouble(el, "kr"),
                    getAttrDouble(el, "kb")
            ));
        }
    }

    private void parseVignetting(Element calibElement, Lens lens) {
        NodeList vigList = calibElement.getElementsByTagName("vignetting");
        for (int i = 0; i < vigList.getLength(); i++) {
            Element el = (Element) vigList.item(i);
            lens.addVignetting(new Lens.Vignetting(
                    getAttrDouble(el, "focal"),
                    getAttrDouble(el, "aperture"),
                    getAttrDouble(el, "distance"),
                    el.getAttribute("model"),
                    getAttrDouble(el, "k1"),
                    getAttrDouble(el, "k2"),
                    getAttrDouble(el, "k3")
            ));
        }
    }


    // Helper to handle empty/missing attributes without crashing
    private double getAttrDouble(Element el, String attr) {
        String val = el.getAttribute(attr);
        if (val == null || val.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public List<Lens> getMasterLensList() {
        return masterLensList;
    }



    // -------------------------------------------------------------------------------------------

    // List all lenses in DB
    public static void main(String[] args) {
        LensDatabaseManager mgr  = new LensDatabaseManager();
        try {
            mgr.loadAllLenses();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        List<Lens> allLenses = mgr.getMasterLensList();
        for (Lens lens : allLenses) {
            System.out.println(lens);
            System.out.println("   Distortions:");
            for (Lens.Distortion d : lens.getDistortions()) {
                System.out.println("      " + d);
            }
            System.out.println("   TCA:");
            for (Lens.Tca tca : lens.getTcaEntries()) {
                System.out.println("      " + tca);
            }
            System.out.println("   Vignetting:");
            for (Lens.Vignetting vig : lens.getVignettingEntries()) {
                System.out.println("      " + vig);
            }
        }

    }

}