package solver;

import java.io.*;
import java.util.*;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import javax.xml.parsers.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ADDAG {
	public static final String dupNodePrefix = "[DUP]";

	public ADNode root;
	public HashMap<String, ADNode> nodes = new HashMap<String, ADNode>();

	private static int lastId = 0;

	/*
	 * Reads a save file from ADTool and creates an ADDAG from it. the saved ADTree
	 * must fulfill some properties: 1) All but one of the instances of a duplicated
	 * label must have no child at all. 2) All duplicated labels must have the above
	 * prefix, to avoid involuntary duplicates in large trees. 3) The tree must
	 * contain the costs for the basic actions. (todo: precise specification)
	 */
	public ADDAG(File f) {
		// init XML reader
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
		} catch (final ParserConfigurationException e) {
			e.printStackTrace();
		}
		Document XMLFile = null;
		try {
			XMLFile = builder.parse(f);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Element XMLRoot = XMLFile.getDocumentElement(); //Renvoi l'élément racine du document
		// Identify domain names

		String costDomainName = null;
		String probDomainName = null;
		String impactDomainName = null;

		// --on trie chaque noeud "domain" dans l'ensemble qui lui correspond
		NodeList domains = XMLRoot.getElementsByTagName("domain");
		for (int i = 0; i < domains.getLength(); i++) {
			Element d = (Element) domains.item(i);
			if (d.getElementsByTagName("name").getLength() != 0) {
				if (d.getElementsByTagName("name").item(0).getTextContent().equals("Investment")) {
					costDomainName = d.getAttribute("id");
				}
				if (d.getElementsByTagName("name").item(0).getTextContent().equals("Impact")) {
					impactDomainName = d.getAttribute("id");
				}
			}
			if (d.getElementsByTagName("class").item(0).getTextContent().contains("ProbSucc")) {
				probDomainName = d.getAttribute("id");
			}
		}

		// --affichage des domaines
		System.out.println("Domains found: " + costDomainName + " , " + probDomainName + " , " + impactDomainName);

		// --s'il n'y a de domaine sur l'arbre passé en paramètre alors on génère un même arbre avec un domaine initialisé à nul pour chaque feuille
		if (costDomainName == null || impactDomainName == null) {
			System.out.println("Incorrect domains detected. Adding proper domains.");
			try {
				BufferedReader input = new BufferedReader(new FileReader(f));
				String newFilePath = f.getAbsolutePath();
				newFilePath = newFilePath.substring(0, newFilePath.length() - 4) + "_updated.xml";
				BufferedWriter output = new BufferedWriter(new FileWriter(newFilePath));
				String currentLine;
				while ((currentLine = input.readLine()) != null) {
					if (currentLine.contains("</adtree>")) {
						int k = currentLine.indexOf("</adtree>");
						output.write(currentLine.substring(0, k));
						if (impactDomainName == null)
							output.write("	<domain id=\"AdtRealDomain41\">\r\n"
									+ "		<class>lu.uni.adtool.domains.custom.AdtRealDomain</class>\r\n"
									+ "		<tool>ADTool2</tool>\r\n" + "		<name>Impact</name>\r\n"
									+ "		<description>Impact</description>\r\n" + "		<op>0.0</op>\r\n"
									+ "		<oo>0.0</oo>\r\n" + "		<ap>0.0</ap>\r\n" + "		<ao>0.0</ao>\r\n"
									+ "		<cp>0.0</cp>\r\n" + "		<co>0.0</co>\r\n"
									+ "		<opponentDefault>0.0</opponentDefault>\r\n"
									+ "		<proponentDefault>0.0</proponentDefault>\r\n"
									+ "		<oppModifiable>true</oppModifiable>\r\n"
									+ "		<proModifiable>true</proModifiable>\r\n" + "	</domain>\r\n");
						if (costDomainName == null)
							output.write("	<domain id=\"AdtRealDomain42\">\r\n"
									+ "		<class>lu.uni.adtool.domains.custom.AdtRealDomain</class>\r\n"
									+ "		<tool>ADTool2</tool>\r\n" + "		<name>Investment</name>\r\n"
									+ "		<description>Investment</description>\r\n" + "		<op>0.0</op>\r\n"
									+ "		<oo>0.0</oo>\r\n" + "		<ap>0.0</ap>\r\n" + "		<ao>0.0</ao>\r\n"
									+ "		<cp>0.0</cp>\r\n" + "		<co>0.0</co>\r\n"
									+ "		<opponentDefault>0.0</opponentDefault>\r\n"
									+ "		<proponentDefault>0.0</proponentDefault>\r\n"
									+ "		<oppModifiable>true</oppModifiable>\r\n"
									+ "		<proModifiable>true</proModifiable>\r\n" + "	</domain>\r\n");

						output.write(currentLine.substring(k));
					} else
						output.write(currentLine);
				}
				output.close();
				input.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.exit(0);
		}

		// --Create the ADDAG : transformation en ensemble A/D

		root = createADDAG((Element) XMLRoot.getElementsByTagName("node").item(0), Actor.A, costDomainName,
				probDomainName, impactDomainName);
		// Now the ADDAG is properly read
	}

	// --transforme les ensembles de noeuds en un arbre pour pouvoir appliquer l'algorithme
	private ADNode createADDAG(Element XMLNode, Actor type, String costDomainName, String probDomainName,
			String impactDomainName) {

		String label = XMLNode.getElementsByTagName("label").item(0).getTextContent();
		double cost = -1, prob = -1, impact = -1;

		NodeList XMLChildrenNodes = XMLNode.getChildNodes();
		ArrayList<ADNode> children = new ArrayList<ADNode>();
		ADNode counter = null;

		//partie récursive
		for (int i = 0; i < XMLChildrenNodes.getLength(); i++) {
			if (XMLChildrenNodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
				if (((Element) XMLChildrenNodes.item(i)).getTagName().equals("node"))
					if (((Element) XMLChildrenNodes.item(i)).getAttribute("switchRole").equals("yes")) {
						if (counter != null)
							throw new IllegalArgumentException("Node " + label + " has several counters!");
						counter = createADDAG((Element) XMLChildrenNodes.item(i), type.other(), costDomainName,
								probDomainName, impactDomainName);
					} else {
						children.add(createADDAG((Element) XMLChildrenNodes.item(i), type, costDomainName,
								probDomainName, impactDomainName));
					}
				if (((Element) XMLChildrenNodes.item(i)).getTagName().equals("parameter")) {
					if (((Element) XMLChildrenNodes.item(i)).getAttribute("domainId").equals(costDomainName))
						cost = Double.parseDouble(((Element) XMLChildrenNodes.item(i)).getTextContent());
					if (((Element) XMLChildrenNodes.item(i)).getAttribute("domainId").equals(probDomainName))
						prob = Double.parseDouble(((Element) XMLChildrenNodes.item(i)).getTextContent());
					if (((Element) XMLChildrenNodes.item(i)).getAttribute("domainId").equals(impactDomainName))
						impact = Double.parseDouble(((Element) XMLChildrenNodes.item(i)).getTextContent());
				}
			}
		}
		Refinement ref;
		switch (XMLNode.getAttribute("refinement")) {
		case "conjunctive":
			ref = Refinement.AND;
			break;
		case "disjunctive":
			ref = Refinement.OR;
			break;
		default:
			throw new IllegalArgumentException("Malformed XML file");
		}
		if (children.isEmpty()) {
			ref = Refinement.N;
		}

		ADNode node = new ADNode(label, children.toArray(new ADNode[children.size()]), counter, ref, type);
		node.cost = cost;
		node.prob = prob;
		node.impact = impact;
		if (this.nodes.containsKey(label))
			if (this.nodes.get(label).deepEquals(node))
				node = this.nodes.get(label);
			else
				throw new IllegalArgumentException("Duplicated node " + label + " have different subtrees");
		this.nodes.put(label, node);
		return node;
	}

	@SuppressWarnings("unused")
	private ADNode mergeNodes(ADNode node1, ADNode node2) {
		/*
		 * if (node1.deepEquals(node2) && node1.label.startsWith(dupNodePrefix)) return
		 * node1;
		 */
		if ((!isMergable(node1)) && (!isMergable(node2)))
			throw new IllegalArgumentException("Cannot merge nodes " + node1 + " and " + node2);
		if (node1.children.length == 0) {
			node1.children = node2.children;
			node1.ref = node2.ref;

		}
		if (node1.counter == null)
			node1.counter = node2.counter;
		return node1;
	}

	private boolean isMergable(ADNode node) {
		if (node.children.length != 0)
			return false;
		if (node.counter != null)
			return false;
		if (!node.label.startsWith(dupNodePrefix))
			return false;
		return true;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("ADDAG containing " + nodes.size() + " nodes\n");
		for (ADNode node : nodes.values()) {
			sb.append(node + " children:" + Arrays.toString(node.children));

			sb.append("counter: " + node.counter + "\n");
		}

		return sb.toString();
	}

	public static ADNode generateRandomADDAG(int numberOfNodes, int maxNumberOfChildren, double childProb,
			double counterProb, int numExtraEdges, Actor act, int r) {
		// Generate a tree then add some extra edges?
		if (numberOfNodes <= 1) {
			ADNode root = new ADNode("Node " + lastId++, new ADNode[0], null, Refinement.N, act);
			root.cost = new Random().nextInt(100) + 1;// Cost between 1 and 100;
			root.rank = r;
			return root;
		}

		ArrayList<ADNode> createdChildren = new ArrayList<ADNode>();
		for (int i = 0; i < maxNumberOfChildren; i++) {
			if (new Random().nextFloat() <= childProb) {
				createdChildren.add(generateRandomADDAG((int) (numberOfNodes / (2 * maxNumberOfChildren * childProb)),
						maxNumberOfChildren, childProb, counterProb,
						(int) (numExtraEdges / (2 * maxNumberOfChildren * childProb)), act, r + 1));
			}
		}
		numExtraEdges /= 2;
		ADNode c = null;
		if (new Random().nextFloat() <= counterProb) {
			c = generateRandomADDAG((int) (numberOfNodes / (2 * counterProb)), maxNumberOfChildren, childProb,
					counterProb, numExtraEdges / 2, act.other(), r + 1);
		}

		Refinement ref = null;
		if (createdChildren.size() == 0) {
			ref = Refinement.N;
		} else {
			if (new Random().nextFloat() < 0.5)
				ref = Refinement.AND;
			else
				ref = Refinement.OR;
		}

		ADNode root = new ADNode("Node " + lastId++, createdChildren.toArray(new ADNode[createdChildren.size()]), c,
				ref, act);
		root.cost = new Random().nextInt(100) + 1;// Cost between 1 and 100;
		root.rank = r;
		// Add some extra edges
		numExtraEdges *= 1 - counterProb / 2;

		ArrayList<ADNode> nodeList = new ArrayList<ADNode>(root.getNodeSet());
		if (r == 0)
			while (numExtraEdges > 0) {
				ADNode a = nodeList.get(new Random().nextInt(nodeList.size()));
				ADNode b = nodeList.get(new Random().nextInt(nodeList.size()));
				if (a.rank >= b.rank)
					continue;
				if (a.type == b.type) {
					ADNode[] newChildren = Arrays.copyOf(a.children, a.children.length + 1);
					newChildren[a.children.length] = b;
					a.children = newChildren;
					numExtraEdges--;
					if (a.ref == Refinement.N)
						a.ref = Refinement.OR;
				} else {
					if (a.counter != null)
						continue;
					a.counter = b;
					numExtraEdges--;
				}

			}

		return root;
	}

	public static ADNode generateRandomADDAG2(int numberOfNodes, double[] childGenLaw, int numExtraEdges,
			double counterProb) {
		if (childGenLaw[0] != 0 || Math.abs(DoubleStream.of(childGenLaw).sum() - 1) > 0.0001)
			throw new IllegalArgumentException("child gen law is incorrect");

		ADNode root = new ADNode("Node " + lastId++, new ADNode[0], null, Refinement.N, Actor.A);
		root.rank = 0;
		while (numberOfNodes > 0) {
			numberOfNodes -= expandLeaf(root, childGenLaw, counterProb);
		}

		// Add extra edges

		ArrayList<ADNode> nodeList = new ArrayList<ADNode>(root.getNodeSet());
		while (numExtraEdges > 0) {
			ADNode a = nodeList.get(new Random().nextInt(nodeList.size()));
			ADNode b = nodeList.get(new Random().nextInt(nodeList.size()));
			if (a.rank + 1 >= b.rank)
				continue;
			if (a.type == b.type) {
				ADNode[] newChildren = Arrays.copyOf(a.children, a.children.length + 1);
				newChildren[a.children.length] = b;
				a.children = newChildren;
				numExtraEdges--;
				if (a.ref == Refinement.N)
					a.ref = Refinement.OR;
			} else {
				if (a.counter != null)
					continue;
				a.counter = b;
				numExtraEdges--;
			}

		}

		return root;
	}

	private static int expandLeaf(ADNode root, double[] childGenLaw, double counterProb) {
		if (root.ref != Refinement.N) {
			int r = new Random().nextInt(root.children.length + (root.counter == null ? 0 : 1));
			if (r < root.children.length) {
				return expandLeaf(root.children[r], childGenLaw, counterProb);
			} else {
				return expandLeaf(root.counter, childGenLaw, counterProb);
			}
		} else {
			ADNode c = null;
			if (new Random().nextDouble() < counterProb) {
				c = new ADNode("Node " + lastId++, new ADNode[0], null, Refinement.N, root.type.other());
				c.rank = root.rank + 1;
				c.cost = new Random().nextInt(100);
			}
			root.counter = c;
			double r = new Random().nextDouble();
			int childNum = -1;
			while (r > 0) {
				childNum++;
				r -= childGenLaw[childNum];
			}
			root.children = new ADNode[childNum];
			for (int i = 0; i < childNum; i++) {
				root.children[i] = new ADNode("Node " + lastId++, new ADNode[0], null, Refinement.N, root.type);
				root.children[i].rank = root.rank + 1;
				root.children[i].cost = new Random().nextInt(100);
			}
			if (childNum != 0)
				root.ref = (new Random().nextDouble() < 0.5 ? Refinement.AND : Refinement.OR);

			return childNum + (c == null ? 0 : 1);
		}

	}

}
