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
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * We will use Files.list to find all .xml files and a single DocumentBuilder to parse them.
 * To prevent the "DTD not found" or "Internet connection" errors we discussed earlier, we will
 * explicitly tell the parser to look for your local lensfun-database.dtd.
 */
public class LensfunDatabase {

    public static final String LOCAL_LENSFUN_DB_PATH = "local_lensfun_db\\";

    private static LensfunDatabase INSTANCE = null;

    private final Set<Camera> uniqueCameras = new TreeSet<>(Comparator.comparing(Camera::getDisplayName));
    private final Map<String, Camera> cameraModelIndex = new HashMap<>();
    private final Map<String, Mount> mountIndex = new HashMap<>();
    private final List<Lens> masterLensList = new ArrayList<>();

    private final Path localDbPath = Path.of(LOCAL_LENSFUN_DB_PATH);

    public static LensfunDatabase getInstance() {
        if (INSTANCE == null) {
            LensfunDatabase db = new LensfunDatabase();
            try {
                db.loadLensfunDatabaseFiles();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            INSTANCE = db;
        }
        return INSTANCE;
    }

    private LensfunDatabase() {}

    public void loadLensfunDatabaseFiles() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Disable DTD validation so it doesn't try to go online
        factory.setValidating(false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        try (var paths = Files.list(localDbPath)) {
            paths.filter(p -> p.toString().endsWith(".xml"))
                    .forEach(path -> {
                        try {
                            parseLensfunFile(builder, path.toFile());
                        } catch (Exception e) {
                            System.err.println("Error parsing " + path + ": " + e.getMessage());
                        }
                    });
        }
        System.out.println("Loaded " + masterLensList.size() + " lenses into memory.");
    }

    private void parseLensfunFile(DocumentBuilder builder, File file) {
        Document doc;
        try {
            doc = builder.parse(file);
        } catch (SAXException | IOException e) {
            throw new RuntimeException(e);
        }
        doc.getDocumentElement().normalize();

        // 1. Process all mount definitions in this file
        NodeList mList = doc.getElementsByTagName("mount");
        for (int i = 0; i < mList.getLength(); i++) {
            Node node = mList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                collectMounts(element);
            }
        }

        // 2. Process all camera definitions in this file
        NodeList cList = doc.getElementsByTagName("camera");
        for (int i = 0; i < cList.getLength(); i++) {
            Node node = cList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element camElem = (Element) node;
                collectCameras(camElem);
            }
        }

        // 3. Process all lens definitions in this file
        NodeList nList = doc.getElementsByTagName("lens");
        for (int i = 0; i < nList.getLength(); i++) {
            Node node = nList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                collectLenses(element);
            }
        }
    }

    // --------------------------------------------------------------------------------------------

    private void collectMounts(Element element) {
        // Ensure this is a TOP-LEVEL mount, not a <lens><mount>
        if (element.getParentNode().getNodeName().equals("lensdatabase")) {
            String name = getTagValue(element, "name");
            List<String> compat = new ArrayList<>();
            NodeList cList = element.getElementsByTagName("compat");
            for (int j = 0; j < cList.getLength(); j++) {
                compat.add(cList.item(j).getTextContent());
            }
            addOrUpdateMount(name, compat);
        }
    }

    private void collectCameras(Element camElem) {
        List<String> makers = getMultiTagValues("maker", camElem);
        List<String> models = getMultiTagValues("model", camElem);
        String primaryMaker = makers.get(0);
        String primaryModel = models.get(0);
        String variant = getTagValue(camElem, "variant"); // helper returns "" if missing
        if (!variant.isEmpty()) {  // e.g., "Nikon D600 (modified)"
            primaryModel += " (" + variant + ")";
        }
        String mount = getTagValue(camElem, "mount");
        double crop = getNumericValue(camElem, "cropfactor", 1.0);

        // Create one object representing the hardware
        Camera cam = new Camera(
                primaryMaker,
                primaryModel,
                makers,
                models,
                mount,
                crop
        );
        // 1. Add to the set for the UI list (The TreeSet keeps it alphabetized)
        uniqueCameras.add(cam);
        // 2. Index by every model name (for searching)
        for (String m : models) {
            cameraModelIndex.put(m.toLowerCase(), cam);
        }
    }

    private void collectLenses(Element lensElement) {
        Lens lens = new Lens();
        lens.setMaker(getTagValue(lensElement, "maker"));
        lens.setModel(getTagValue(lensElement, "model"));
        lens.setType(getTagValue(lensElement, "type"));
        lens.setCropFactor(getNumericValue(lensElement, "cropfactor", 1.0));

        // --- retrieve all mounts ---
        // NodeList mountList = element.getElementsByTagName("mount");
        // for (int j = 0; j < mountList.getLength(); j++) {
        //     String mountName = mountList.item(j).getTextContent();
        //     lens.addMount(mountName);
        // }

        List<String> mntNames = parseMounts(lensElement);
        System.out.println("***************** " + lens.getModel() + " mounts: " + Arrays.toString(mntNames.toArray()));
        lens.addMount(mntNames);
        lens.setAspectRatio(parseAspectRatio(lensElement));
        lens.addDistortion(parseDistortion(lensElement));
        lens.addTca(parseTca(lensElement));
        lens.addVignetting(parseVignetting(lensElement));

        lens.setFocalRange(parseRange(lensElement, "focal"));
        lens.setApertureRange(parseRange(lensElement, "aperture"));

        masterLensList.add(lens);
    }

    // ---------------------------------------------------------------------------------------------

    private String getTagValue(Element element, String tagName) {
        NodeList list = element.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent();
        }
        return "";
    }

    /*
    Since we previously discussed that <maker> and <model> can appear multiple times for aliases
    (like the Canon Rebel/800D example), you should ensure your "Multi" helper doesn't accidentally
    treat a translation as a separate alias.
    The Rule of Thumb: * Different Values = Aliases (Keep both).
    Same Value, different lang = Translation (Keep only one).
     */
    private List<String> getMultiTagValues(String tagName, Element element) {
        Set<String> uniqueValues = new LinkedHashSet<>(); // Maintains order, prevents duplicates
        NodeList nl = element.getElementsByTagName(tagName);

        for (int i = 0; i < nl.getLength(); i++) {
            Element el = (Element) nl.item(i);
            String val = el.getTextContent().trim();
            String lang = el.getAttribute("lang");

            // Only add if it's the primary/English version or we don't have this value yet
            if (lang.isEmpty() || lang.equals("en") || !uniqueValues.contains(val)) {
                uniqueValues.add(val);
            }
        }
        return new ArrayList<>(uniqueValues);
    }

    private List<String> parseMounts(Element element) {
        NodeList mountNodes = element.getElementsByTagName("mount");
        List<String> mounts = new ArrayList<>();
        for (int j = 0; j < mountNodes.getLength(); j++) {
            String mountName = mountNodes.item(j).getTextContent();
            mounts.add(mountName);
        }
        // System.out.println("***************** mounts: " + Arrays.toString(mounts.toArray()));
        return mounts;
    }

    private List<Lens.Distortion> parseDistortion(Element calibElement) {
        NodeList distNodes = calibElement.getElementsByTagName("distortion");
        List<Lens.Distortion> distList = new ArrayList<>();

        for (int k = 0; k < distNodes.getLength(); k++) {
            Element distElem = (Element) distNodes.item(k);

            String model = distElem.getAttribute("model");
            double focal = getNumericAttributeValue(distElem, "focal");

            // Pull all possible coefficients; non-existent ones return 0.0
            double k1 = getNumericAttributeValue(distElem, "k1");
            double k2 = getNumericAttributeValue(distElem, "k2");
            double a  = getNumericAttributeValue(distElem, "a");
            double b  = getNumericAttributeValue(distElem, "b");
            double c  = getNumericAttributeValue(distElem, "c");

            distList.add(new Lens.Distortion(focal, model, k1, k2, a, b, c));
        }
        return distList;
    }

    // <tca model="poly3" focal="18" br="-0.0000385" vr="1.0001447" bb="0.0000468" vb="0.9999362"/>
    private List<Lens.Tca> parseTca(Element calibElement) {
        NodeList tcaNodes = calibElement.getElementsByTagName("tca");
        List<Lens.Tca> tcaList = new ArrayList<>();
        for (int i = 0; i < tcaNodes.getLength(); i++) {
            Element el = (Element) tcaNodes.item(i);
            tcaList.add(new Lens.Tca(
                    el.getAttribute("model"),
                    getNumericAttributeValue(el, "focal"),
                    // model = linear:
                    getNumericAttributeValue(el, "kr"),
                    getNumericAttributeValue(el, "kr"),
                    // model = poly3:
                    getNumericAttributeValue(el, "vr"),
                    getNumericAttributeValue(el, "vb"),
                    getNumericAttributeValue(el, "cr"),
                    getNumericAttributeValue(el, "cb"),
                    getNumericAttributeValue(el, "br"),
                    getNumericAttributeValue(el, "bb")
            ));
        }
        return tcaList;
    }

    private List<Lens.Vignetting> parseVignetting(Element calibElement) {
        NodeList vigNodes = calibElement.getElementsByTagName("vignetting");
        List<Lens.Vignetting> vignettingList = new ArrayList<>();
        for (int i = 0; i < vigNodes.getLength(); i++) {
            Element el = (Element) vigNodes.item(i);
            vignettingList.add(new Lens.Vignetting(
                    el.getAttribute("model"),
                    getNumericAttributeValue(el, "focal"),
                    getNumericAttributeValue(el, "aperture"),
                    getNumericAttributeValue(el, "distance"),
                    getNumericAttributeValue(el, "k1"),
                    getNumericAttributeValue(el, "k2"),
                    getNumericAttributeValue(el, "k3")
            ));
        }
        return vignettingList;
    }

    private Lens.AspectRatio parseAspectRatio(Element lensElement) {
        NodeList nl = lensElement.getElementsByTagName("aspect-ratio");
        if (nl.getLength() > 0) {
            String raw = nl.item(0).getTextContent();
            if (raw.contains(":")) {
                String[] parts = raw.split(":");
                return (new Lens.AspectRatio(
                        Integer.parseInt(parts[0].trim()),
                        Integer.parseInt(parts[1].trim())));
            }
        }
        return new Lens.AspectRatio(3, 2);    // Standard Lensfun default
    }


    // Helper to handle empty/missing attributes without crashing
    private double getNumericAttributeValue(Element el, String attr) {
        String val = el.getAttribute(attr);
        if (val == null || val.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Smart numeric value reader
     * @param element
     * @param key
     * @param defaultValue
     * @return
     */
    private double getNumericValue(Element element, String key, double defaultValue) {
        // 1. Check for Child Element first (e.g., <cropfactor>1.5</cropfactor>)
        NodeList nl = element.getElementsByTagName(key);
        if (nl.getLength() > 0) {
            return Double.parseDouble(nl.item(0).getTextContent());
        }
        // 2. Fallback to Attribute (e.g., focal="50")
        if (element.hasAttribute(key)) {
            return Double.parseDouble(element.getAttribute(key));
        }
        return defaultValue;
    }

    private Lens.NumericRange parseRange(Element lensElement, String tagName) {
        NodeList nl = lensElement.getElementsByTagName(tagName);
        if (nl.getLength() > 0) {
            Element el = (Element) nl.item(0);

            try {
                // Case 1: Standard min/max attributes
                if (el.hasAttribute("min") && el.hasAttribute("max")) {
                    double min = Double.parseDouble(el.getAttribute("min"));
                    double max = Double.parseDouble(el.getAttribute("max"));
                    return new Lens.NumericRange(min, max);
                }

                // Case 2: Fixed value (common in older lensfun entries)
                if (el.hasAttribute("value")) {
                    double val = Double.parseDouble(el.getAttribute("value"));
                    return new Lens.NumericRange(val, val);
                }
            } catch (NumberFormatException e) {
                System.err.println("Could not parse range for " + tagName);
            }
        }
        return new Lens.NumericRange(0, 0); // Unknown/Missing
    }

    public List<Lens> getMasterLensList() {
        return masterLensList;
    }

    void addOrUpdateMount(String name, List<String> newCompats) {
        mountIndex.compute(name, (key, existing) -> {
            if (existing == null) {
                return new Mount(name, new ArrayList<>(newCompats));
            } else {
                // Add any new unique compatible mounts to the existing list
                for (String compat : newCompats) {
                    List<String> exCompats = existing.compatibleWith();
                    if (!exCompats.contains(compat)) {
                        exCompats.add(compat);
                    }
                }
                return existing;
            }
        });
    }

    public List<Lens> findCompatibleLenses(Camera camera) {
        // 1. Get the primary mount of the camera
        String cameraMount = camera.mount();

        // 2. Identify all acceptable mounts (Native + Compatible)
        Set<String> acceptable = new HashSet<>();
        acceptable.add(cameraMount);

        Mount def = this.mountIndex.get(cameraMount);
        if (def != null) {
            acceptable.addAll(def.compatibleWith());
        }

        // 3. Filter the master lens list
        return masterLensList.stream()
                .filter(lens -> lens.getMounts().stream().anyMatch(acceptable::contains))
                .sorted(Comparator.comparing(Lens::getModel))
                .toList();
    }

    public List<Camera> getCamerasByMaker(String maker) {
        return uniqueCameras.stream()
                .filter(cam -> cam.primaryMaker().equalsIgnoreCase(maker))
                .toList(); // TreeSet handles the sorting for us
    }

    public List<Lens> getLensesByMaker(String maker) {
        return this.masterLensList.stream()
                .filter(lens -> lens.getMaker().equalsIgnoreCase(maker))
                .toList(); // TreeSet handles the sorting for us
    }

    // -------------------------------------------------------------------------------------------

    // List all lenses in DB
    static void listAllLenses() {
        LensfunDatabase db = LensfunDatabase.getInstance();

        List<Lens> allLenses = db.getMasterLensList();
        for (Lens lens : allLenses) {
            // System.out.println(lens);
            lens.print();
            // System.out.println("   Aspect ratio: " + lens.getAspectRatio());
            // System.out.println("   Distortions:");
            // for (Lens.Distortion d : lens.getDistortions()) {
            //     System.out.println("      " + d);
            // }
            // System.out.println("   TCA:");
            // for (Lens.Tca tca : lens.getTcaEntries()) {
            //     System.out.println("      " + tca);
            // }
            // System.out.println("   Vignetting:");
            // for (Lens.Vignetting vig : lens.getVignettingEntries()) {
            //     System.out.println("      " + vig);
            // }
        }

    }

    static void findLens(String query) {
        LensfunDatabase db = LensfunDatabase.getInstance();
        List<Lens> allLenses = db.getMasterLensList();
        for (Lens lens : allLenses) {
            if (lens.matches(query)) {
                lens.print();
            }
        }
    }

    static void listMounts() {
        LensfunDatabase db = LensfunDatabase.getInstance();
        System.out.println("Mounts " + db.mountIndex.size());
        for (Mount md : db.mountIndex.values()) {
            System.out.println(md);
        }
    }

    static void listUniqueCameras() {
        LensfunDatabase db = LensfunDatabase.getInstance();
        System.out.println("Unique cameras" + db.uniqueCameras.size());

        List<Camera> camList = new ArrayList<>(db.uniqueCameras);
        // List<Camera> camList = db.uniqueCameras.stream().toList();
        for (Camera cam : camList) {
            // System.out.println(cam.primaryMaker() + " " + cam.primaryModel());
            System.out.println(cam);
        }
        // Since uniqueCameras is a TreeSet, this is already sorted and unique
    }

    /*
    The Menu: You populate your ComboBox using uniqueCameras.stream().toList(). The user sees
    "Canon EOS 800D" exactly once. The Search Box: When the user types "Rebel T7i", you do a quick
    modelIndex.get("rebel t7i".toLowerCase()). It returns the Camera object, and you can instantly
    select the correct item in your menu.
     */
    static void listCameraIndex() {
        LensfunDatabase db = LensfunDatabase.getInstance();
        System.out.println("Camera index: " + db.cameraModelIndex.size());
        for (String name : db.cameraModelIndex.keySet()) {
            System.out.println(name);
        }
    }

    static void listCameraMakers() {
        LensfunDatabase db = LensfunDatabase.getInstance();
        List<String> allCamMakers = db.uniqueCameras.stream()
                .map(Camera::primaryMaker)
                .distinct()
                .sorted()
                .toList();
        System.out.println("All camera makers: " + db.uniqueCameras.size());
        for (String name : allCamMakers) {
            System.out.println(name);
        }
    }

    static void listCamerasByMaker(String maker) {
        LensfunDatabase db = LensfunDatabase.getInstance();
        List<Camera> cams = db.getCamerasByMaker(maker);
        for (Camera cam : cams) {
            System.out.println("   " + cam.primaryModel());
        }
    }

    static void listLensesByMaker(String maker) {
        LensfunDatabase db = LensfunDatabase.getInstance();
        List<Lens> lenses = db.getLensesByMaker(maker);
        for (Lens lens : lenses) {
            lens.print();
            // System.out.println("   " + lens.getModel());
        }
    }

    static void listCompatibleLenses(String camName) {
        LensfunDatabase db = LensfunDatabase.getInstance();
        Camera cam = db.cameraModelIndex.get(camName.toLowerCase());
        if (cam == null) {
            System.out.println(camName + " not found!");
            return;
        }
        List<Lens> lenses = db.findCompatibleLenses(cam);
        for (Lens lens : lenses) {
            System.out.println("  " + lens);
        }
    }

    // ------------------

    public static void main(String[] args) {
        // listAllLenses();
        findLens("16-300mm F3.5-6.7 DC OS | Contemporary 025");
        // listMounts();
        // listUniqueCameras();
        // listCameraIndex();
        // listCameraMakers();
        // listCompatibleLenses("alpha 6500");
        // listCamerasByMaker("Nikon Corporation");
        // listLensesByMaker("GoPro");
    }


}