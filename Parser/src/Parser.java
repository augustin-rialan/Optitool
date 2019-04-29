import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Parser {


    public static void main(String[] args) throws IOException {
        // Nous récupérons une instance de factory qui se chargera de nous fournir
        // un parseur
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        try {
            // Création de notre parseur via la factory
            DocumentBuilder builder = factory.newDocumentBuilder();
            File fileXML = new File("mon_xml.xml");

            // parsing de notre fichier via un objet File et récupération d'un
            // objet Document
            // Ce dernier représente la hiérarchie d'objet créée pendant le parsing
            Document xml = builder.parse(fileXML);

            // Via notre objet Document, nous pouvons récupérer un objet Element
            // Ce dernier représente un élément XML mais, avec la méthode ci-dessous,
            // cet élément sera la racine du document
            Element root = xml.getDocumentElement();
            System.out.println(root.getNodeName());
            NodeList listeLabel = root.getElementsByTagName("label");
            for ( int i = 0; i < listeLabel.getLength(); i++) {
                //System.out.println(listeLabel.item(i).getTextContent());
            }


        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        InputStream flux;

        {
            try {
                flux = new FileInputStream("dag.txt");
                InputStreamReader lecture=new InputStreamReader(flux);
                BufferedReader buff=new BufferedReader(lecture);
                /*
                while (buff.readLine() != null){
                    System.out.println(buff.readLine());
                }
                */
                Pattern pattern = Pattern.compile("Best defense for");
                String ligne;
                while ( (ligne = buff.readLine()) != null){
                    Matcher matcher = pattern.matcher(ligne);
                    if (matcher.find()){
                        String [] tab = buff.readLine().split(",");
                        System.out.println(tab[0]);
                    }

                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }


    }

}
