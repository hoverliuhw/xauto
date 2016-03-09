import java.io.*;
import java.util.*;
import java.util.regex.*;

enum CheckResult {
	EQUAL, NOTEQUAL, NOTFOUND
}

class CantParseException extends Exception {
	// This Exception is for bracket missmatch issue, such as mapss7! message
}

/*
 * BooleanClass is defined for "subscript at"
 * because traditional Boolean class can't change value, 
 */
class BooleanClass {
	public boolean flag;
	
	BooleanClass(boolean flag) {
		this.flag = flag;
	}
}

class TNode {
	public String key;
	public String value;
	public List<TNode> subNodes;

	public TNode() {
		key = null;
		value = null;
		subNodes = new ArrayList<TNode>();

	}

	public TNode(String sKey, String sValue, List<TNode> sNode) {
		key = sKey;
		value = sValue;
		subNodes = sNode;

	}

	public boolean isLeaf() {
		return ((this.subNodes == null) || (this.subNodes.isEmpty()));
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder(key);
		if (value != null) {
			sb.append("=" + value);
		}
		
		if (!isLeaf()) {
			sb.append("(");
			Iterator<TNode> it = subNodes.iterator();
			while (it.hasNext()) {
				TNode child = it.next();
				sb.append(child.toString());
				if (it.hasNext()) {
					sb.append(",");
				}
			}
			sb.append(")");
		}
		return sb.toString();
	}

}

class RNode {
	public CheckResult checkResult;
	public String actualValue;
	public String key;
	public String value;
	public List<RNode> subNodes;

	public RNode() {
		key = null;
		value = null;
		subNodes = new ArrayList<RNode>();
		checkResult = CheckResult.NOTFOUND;
		actualValue = null;

	}

	public RNode(String sKey, String sValue, List<RNode> sNode) {
		key = sKey;
		value = sValue;
		subNodes = sNode;
		checkResult = CheckResult.NOTFOUND;
		actualValue = null;

	}

	public boolean isLeaf() {
		return ((this.subNodes == null) || (this.subNodes.isEmpty()));
	}
	
	public String genTabs(int num) {
		StringBuilder sbTabs = new StringBuilder();
		while (num > 0) {
			sbTabs.append("    ");
			num--;
		}
		
		return sbTabs.toString();
		
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder(key);
		if (value != null) {
			sb.append("=" + value);
		}
		sb.append(" -> " + checkResult);
		if (checkResult == CheckResult.NOTEQUAL) {
			sb.append("(actualValue=" + actualValue + ")");
		}
		
		if (!isLeaf()) {
			sb.append("(");
			
			Iterator<RNode> it = subNodes.iterator();
			while (it.hasNext()) {
				RNode child = it.next();
				sb.append(child.toString());
				if (it.hasNext()) {
					sb.append(",");
				}
			}
			
			sb.append(")");
		}
		return sb.toString();
	}
	
	public String toTreeString(int deep) {
		String beginTabs = genTabs(deep);
		StringBuilder sb = new StringBuilder(beginTabs);
		sb.append(key);
		if (value != null) {
			sb.append("=" + value);
		}
		if (checkResult == CheckResult.NOTEQUAL) {
			sb.append(" -> " + checkResult + " (" + actualValue + ")\n");
		} else {
			sb.append(" -> " + checkResult + "\n");
		}
		
		if (!isLeaf()) {
			sb.append(beginTabs + "(\n");
			Iterator<RNode> it = subNodes.iterator();
			while (it.hasNext()) {
				RNode child = it.next();
				sb.append(child.toTreeString(deep + 1));
			}
			sb.append(beginTabs + ")\n");
		}
		
		return sb.toString();
	}
	
	public void reset() {
		checkResult = CheckResult.NOTFOUND;
		actualValue = null;
		if (!isLeaf()) {
			Iterator<RNode> it = subNodes.iterator();
			while (it.hasNext()) {
				RNode child = it.next();
				child.reset();
			}
		}
	}
	
	public void setToEqual() {
		checkResult = CheckResult.EQUAL;
		actualValue = null;
		if (!isLeaf()) {
			Iterator<RNode> it = subNodes.iterator();
			while (it.hasNext()) {
				RNode child = it.next();
				child.setToEqual();
			}
		}
	}

}

public class LogParser {
	private XController controller = null;
	private static final Pattern OBJ_PATTERN = Pattern.compile("index=[0-9]\\s+[a-zA-Z_]+");
	private static final Pattern AMA_PATTERN = Pattern.compile("AMA_[^_]+_Generation");

	public LogParser(XController c) {
		controller = c;
	}

	public LogParser() {

	}

	public void setController(XController c) {
		controller = c;
	}
	
	public int findEndBracket(String str, int start) {
		int end = -1, left = 0, i = start;
		char ch;
		while (i < str.length()) {
			ch = str.charAt(i);
			if (ch == '(') {
				left++;
			} else if (ch == ')') {
				left--;
				if (left == 0) {
					end = i;
					break;
				}
			}

			i++;
		}

		return end;
	}

	public boolean isMidComma(String str, int start) {
		int iComma = str.indexOf(",", start);
		int iEqual = str.indexOf("=", start);

		// First half is case 1: this node is not the last one
		// Second half is case 2: this node is the last, there wont be comma or equal
		// return (((iComma > -1) && (iComma < iEqual)) || ((iComma == -1) && (iEqual == -1)));
		return (((iComma > -1) && (iComma < iEqual)) || (iEqual == -1));
	}
	
	public boolean isObjectString(String value) {
		// Original string is "^index=[0-9]+  [z-zA-z_]+\("
		// String reg = "index=1\\s+[a-zA-Z_]+";
		// Pattern p = Pattern.compile(reg);
		Matcher m = OBJ_PATTERN.matcher(value);
		
		return m.matches();
	}

	public boolean isAMA(String trace) {
		Matcher m = AMA_PATTERN.matcher(trace);
		return m.matches();
	}

	public void appendToTree(TNode root, String str) {
		// System.out.println("String to process: " + str);
		String key = null, value = null;
		TNode tmpNode = null;

		int start = 0, end = 0, index = 0;
		int len = str.length();
		char ch;

		while (index < len && start < len && end <= len) {
			ch = str.charAt(index);
			// System.out.println("\t Char to process: " + ch
			// + ", index is: " + index + ", start is: " + start + ", end is: "
			// + end );
			if (ch == '=' && key == null) {
				// need check whether '=' is a char inside a string value, not
				// real equal
				end = index;
				key = str.substring(start, end);
				start = end + 1;
				index++;
			} else if (ch == ',') {
				if (isMidComma(str, index + 1)) {
					// To judge whether the comma is inside a string value, not
					// end of a field
					// meanwhile, a dot "." maybe the end of a field sometimes,
					// such as in send om
					// index = str.indexOf(",", index + 1);
					index++;
				} else {
					end = index;
					value = str.substring(start, end);
					//root.subNodes.put(key, new TNode(key, value,
					//		new HashMap<String, TNode>()));
					root.subNodes.add(new TNode(key, value, new ArrayList<TNode>()));
					key = null;
					index++;
					start = index;
				}
			} else if (ch == '(') {
				end = index;
				value = str.substring(start, end);
				
				if (isObjectString(value)) {
					tmpNode = new TNode(key, "", new ArrayList<TNode>());
					root.subNodes.add(tmpNode);
					key = null;
					end = appendObjectToTree(tmpNode, str, start);
					start = end + 1;
					index = end + 1;
				} else {
					end = findEndBracket(str, index);
					String substr = str.substring(index + 1, end);
					/* Add checking value string contains string like "(1)" */
					if (!substr.contains("=")) {
						index = end + 1;
						continue;
					}
					/*before here, variable start should not be set to other value*/
					tmpNode = new TNode(key, value, new ArrayList<TNode>());
					root.subNodes.add(tmpNode);
					key = null;
					appendToTree(tmpNode, substr);
					start = end + 2;
					index = end + 2;
				}
				
			} else {
				index++;
			}
		}

		if (!str.endsWith(")")) {
			end = len;
			value = str.substring(start, end);
			root.subNodes.add(new TNode(key, value,
					new ArrayList<TNode>()));
		}

	}
	
	/***	
	 *  root should be a new node created in previous function,
	 *  start should be the index after "=".
	 *  return end of object, either end of whole string, or comma
	 */
	public int appendObjectToTree(TNode root, String str, int start) {
		// root should be a new node created in previous function, 
		// start should be the index after "=", 
		// it should return end of object, either end of whole string, or comma
		int index = start;
		int end = start + 1;
		while (index < str.length()) {
			start = index;
			index = str.indexOf("index=", start);
			if (index == start) {
				index = str.indexOf("(", start);
				String key = str.substring(start, index);
				
				end = findEndBracket(str, index);				
				TNode tmpNode = new TNode(key, "", new ArrayList<TNode>());
				//value should be null or empty?
				
				start = index + 1;
				appendToTree(tmpNode, str.substring(start, end));
				root.subNodes.add(tmpNode);
				
				index = end + 1;
			} else {
				end = start;
				break;
			}
		}
		return end;
		
	}

	public void appendToTree(RNode root, String str) {

		String key = null, value = null;
		RNode tmpNode = null;

		int start = 0, end = 0, index = 0;
		int len = str.length();
		char ch;

		while (index < len && start < len && end <= len) {
			ch = str.charAt(index);
			if (ch == '=' && key == null) {
				// if key is not null, then this equal is part of string
				end = index;
				key = str.substring(start, end);
				start = end + 1;
				index++;
			} else if (ch == ',') {
				if (isMidComma(str, index + 1)) {
					// To judge whether the comma is inside a string value, not
					// end of a field
					// meanwhile, a dot "." maybe the end of a field sometimes,
					// such as in send om
					// index = str.indexOf(",", index + 1);
					index++;
				} else {
					end = index;
					value = str.substring(start, end);
					root.subNodes.add(new RNode(key, value,
							new ArrayList<RNode>()));
					key = null;
					index++;
					start = index;
				}
			} else if (ch == '(') {
				end = index;
				value = str.substring(start, end);
				
				if (isObjectString(value)) {
					tmpNode = new RNode(key, "", new ArrayList<RNode>());
					root.subNodes.add(tmpNode);
					key = null;
					end = appendObjectToTree(tmpNode, str, start);
					start = end + 1;
					index = end + 1;
				} else {
					end = findEndBracket(str, index);
					String substr = str.substring(index + 1, end);
					/* Add checking value string contains string like "(1)" */
					if (!substr.contains("=")) {
						index = end + 1;
						continue;
					}
					/*Before here, start should not be set to any value*/
					tmpNode = new RNode(key, value, new ArrayList<RNode>());
					root.subNodes.add(tmpNode);
					key = null;
					appendToTree(tmpNode, substr);
					start = end + 2;
					index = end + 2;
				}
				
			} else {
				index++;
			}
		}

		if (!str.endsWith(")")) {
			/*if a string ends with (1), then we have no way to parse it out!!!*/
			end = len;
			value = str.substring(start, end);
			root.subNodes.add(new RNode(key, value,
					new ArrayList<RNode>()));
		}

	}
	
	/***	
	 *  appendObjectToTree RNode version
	 *  in the future, this function will be replaced with template
	 */
	public int appendObjectToTree(RNode root, String str, int start) {
		// root should be a new node created in previous function, 
		// start should be the index after "=", 
		// it should return end of object, either end of whole string, or comma
		int index = start;
		int end = start + 1;
		while (index < str.length()) {
			start = index;
			index = str.indexOf("index=", start);
			if (index == start) {
				index = str.indexOf("(", start);
				String key = str.substring(start, index);
				
				end = findEndBracket(str, index);				
				RNode tmpNode = new RNode(key, "", new ArrayList<RNode>());
				
				start = index + 1;
				appendToTree(tmpNode, str.substring(start, end));
				root.subNodes.add(tmpNode);
				
				index = end + 1;
			} else {
				end = start;
				break;
			}
		}
		return end;
		
	}


	public void parseSendOM(String str, TNode root) {
		String key = null;
		String value = null;
		TNode tmpNode = null;

		int start = 0, end = 0, index = 0;
		int len = str.length();
		char ch;

		while (index < len && start < len && end < len) {
			ch = str.charAt(index);

			if (ch == '=' && key == null) {
				end = index;
				key = str.substring(start, end);
				index++;
				start = index;
			} else if (ch == ',') {
				end = index;
				value = str.substring(start, end);
				tmpNode = new TNode(key, value, new ArrayList<TNode>());
				root.subNodes.add(tmpNode);
				key = null;
				index++;
				start = index;
			} else {
				index++;
			}

			if (key != null && key.equals("message")) {
				end = str.indexOf("message2=");
				end--;
				value = str.substring(start, end);
				tmpNode = new TNode(key, value, new ArrayList<TNode>());
				root.subNodes.add(tmpNode);

				key = new String("message2");
				start = end + key.length() + 2;
				end = len;
				value = str.substring(start, end);
				tmpNode = new TNode(key, value, new ArrayList<TNode>());
				root.subNodes.add(tmpNode);
				break;
			}
		}

	}

	public void parseSendOM(String str, RNode root) {
		String key = null;
		String value = null;
		RNode tmpNode = null;

		int start = 0, end = 0, index = 0;
		int len = str.length();
		char ch;

		while (index < len && start < len && end < len) {
			ch = str.charAt(index);
			if (ch == '=') {
				end = index;
				key = str.substring(start, end);
				index++;
				start = index;
			} else if (ch == ',') {
				end = index;
				value = str.substring(start, end);
				tmpNode = new RNode(key, value, new ArrayList<RNode>());
				root.subNodes.add(tmpNode);
				index++;
				start = index;
			} else {
				index++;
			}

			if (key != null && key.equals("message")) {
				end = str.indexOf("message2");
				if (end > -1) {
					end--;
					value = str.substring(start, end);
					tmpNode = new RNode(key, value,
							new ArrayList<RNode>());
					root.subNodes.add(tmpNode);

					key = new String("message2");
					start = end + key.length() + 2;
					end = len;
					value = str.substring(start, end);
					tmpNode = new RNode(key, value,
							new ArrayList<RNode>());
					root.subNodes.add(tmpNode);
				} else {
					end = len;
					value = str.substring(start, end);
					tmpNode = new RNode(key, value,
							new ArrayList<RNode>());
					root.subNodes.add(tmpNode);
				}
				break;
			}
		}

		if (end < len) {
			end = len;
			value = str.substring(start, end);
			root.subNodes.add(new RNode(key, value,
					new ArrayList<RNode>()));
		}

	}

	public void parseTextProcessResult(String str, TNode root) {
		String key = null, value = null;
		TNode tmpNode = null;

		int start = 0, end = 0, index = 0;
		int len = str.length();
		char ch;

		while (start < len && end < len && index < len) {
			ch = str.charAt(index);

			if (ch == '=') {
				end = index;
				key = str.substring(start, end);
				index++;
				start = index;
			} else if (ch == ',') {
				end = index;
				value = str.substring(start, end);
				tmpNode = new TNode(key, value, new ArrayList<TNode>());
				root.subNodes.add(tmpNode);
				index++;
				start = index;
			} else {
				index++;
			}

			if (key != null && key.equals("Text_String")) {
				end = str.indexOf("Last_Message_Flag");
				end--;
				value = str.substring(start, end);
				tmpNode = new TNode(key, value, new ArrayList<TNode>());
				root.subNodes.add(tmpNode);

				key = new String("Last_Message_Flag");
				start = end + key.length() + 2;
				end = len;
				value = str.substring(start, end);
				tmpNode = new TNode(key, value, new ArrayList<TNode>());
				root.subNodes.add(tmpNode);
				break;
			}
		}

	}

	public void parseTextProcessResult(String str, RNode root) {
		String key = null;
		String value = null;
		RNode tmpNode = null;

		int start = 0, end = 0, index = 0;
		int len = str.length();
		char ch;

		while (index < len && start < len && end < len) {
			ch = str.charAt(index);
			if (ch == '=') {
				end = index;
				key = str.substring(start, end);
				index++;
				start = index;
			} else if (ch == ',') {
				end = index;
				value = str.substring(start, end);
				tmpNode = new RNode(key, value, new ArrayList<RNode>());
				root.subNodes.add(tmpNode);
				index++;
				start = index;
			} else {
				index++;
			}

			if (key != null && key.equals("Text_String")) {
				end = str.indexOf("Last_Message_Flag");
				if (end > -1) {
					end--;
					value = str.substring(start, end);
					tmpNode = new RNode(key, value,
							new ArrayList<RNode>());
					root.subNodes.add(tmpNode);

					key = new String("Last_Message_Flag");
					start = end + key.length() + 2;
					end = len;
					value = str.substring(start, end);
					tmpNode = new RNode(key, value,
							new ArrayList<RNode>());
					root.subNodes.add(tmpNode);
				} else {
					end = len;
					value = str.substring(start, end);
					tmpNode = new RNode(key, value,
							new ArrayList<RNode>());
					root.subNodes.add(tmpNode);
				}
				break;
			}
		}

		if (end < len) {
			end = len;
			value = str.substring(start, end);
			root.subNodes.add(new RNode(key, value,
					new ArrayList<RNode>()));
		}

	}

	public void printTree(TNode root) {
		String tKey = root.key;
		String tValue = null;
		if (root.value != null) {
			tValue = "=" + root.value;
		} else {
			tValue = "";
		}
		System.out.print(tKey + tValue);
		if (!root.isLeaf()) {
			System.out.print("(");
			Iterator<TNode> it = root.subNodes.iterator();

			while (it.hasNext()) {
				TNode child = it.next();
				printTree(child);
				if (it.hasNext()) {
					System.out.print(",");
				}
			}
			System.out.print(")");
		}

	}

	public void printTree(RNode root) {
		String tKey = root.key;
		String tValue = null;
		if (root.value != null) {
			tValue = "=" + root.value + ", " + root.checkResult;
		} else {
			tValue = ", " + root.checkResult;
		}
		System.out.print(tKey + tValue);
		if (!root.isLeaf()) {
			System.out.print("(");
			Iterator<RNode> it = root.subNodes.iterator();

			while (it.hasNext()) {
				RNode child = it.next();
				printTree(child);
				if (it.hasNext()) {
					System.out.print(",");
				}
			}
			System.out.print(")");
		}

	}

	public String treeToString(TNode root) {
		String tKey = root.key;
		String tValue = null;
		StringBuilder sb = new StringBuilder();

		if (root.value != null) {
			tValue = "=" + root.value;
		} else {
			tValue = "";
		}

		sb.append(tKey + tValue);

		if (!root.isLeaf()) {
			sb.append("(");
			Iterator<TNode> it = root.subNodes.iterator();
			while (it.hasNext()) {
				TNode child = it.next();
				sb.append(treeToString(child));
				if (it.hasNext()) {
					sb.append(",");
				}
			}
			sb.append(")");
		}

		return sb.toString();
	}

	public String treeToString(RNode root) {
		String tKey = root.key;
		String tValue = null;
		StringBuilder sb = new StringBuilder();

		if (root.value != null) {
			tValue = "=" + root.value;
		} else {
			tValue = "";
		}

		sb.append(tKey + tValue + " -> " + root.checkResult);

		if (!root.isLeaf()) {
			sb.append("(");
			Iterator<RNode> it = root.subNodes.iterator();
			while (it.hasNext()) {
				RNode child = it.next();
				sb.append(treeToString(child));
				if (it.hasNext()) {
					sb.append(",");
				}
			}
			sb.append(")");
		}

		return sb.toString();
	}

	public void genTree(TNode root, String str) {
		int start = 0, end = 0;

		end = str.indexOf("(");
		if (end > -1) {
			root.key = str.substring(0, end);
			start = end + 1;
			end = str.length() - 1;
			if (end > start) {
				//if (root.key.equals("send_om")) {
				//	parseSendOM(str.substring(start, end), root);
				//} else {
					appendToTree(root, str.substring(start, end));
				//}
			}
		} else {
			root.key = str;
		}

	}

	public void genTree(RNode root, String str) {
		int start = 0, end = 0;

		end = str.indexOf("(");
		if (end > -1) {
			root.key = str.substring(0, end);
			start = end + 1;
			end = str.length() - 1;
			if (end > start) {
				//if (root.key.equals("send_om")) {
				//	parseSendOM(str.substring(start, end), root);
				//} else {
					appendToTree(root, str.substring(start, end));
				//}
			}
		} else {
			root.key = str;
		}

	}

	public void compNodeInTree(RNode rn, TNode root) {
		TNode child = null;
		Iterator<TNode> it = root.subNodes.iterator();
		while (it.hasNext()) {
			if (rn.checkResult == CheckResult.EQUAL) {
				break; // to avoid duplicate check such as in SIM_RTDB!update
			}

			child = it.next();

			if (child.isLeaf()) {
				if (rn.key.equals(child.key)) {
					String varStr = "$VARIABLE";
					if (rn.value.contains(varStr)) {
						/*
						int startOfVar = rn.value.indexOf(varStr);
						int endOfVar = startOfVar + varStr.length();
						String preVar = rn.value.substring(0, startOfVar);
						String postVar = "";
						if (endOfVar < rn.value.length()) {
							postVar = rn.value.substring(endOfVar + 1);
						}
						if ((child.value != null) && child.value.startsWith(preVar) && child.value.endsWith(postVar)) {
							rn.checkResult = CheckResult.EQUAL;
						} else {
							rn.checkResult = CheckResult.NOTEQUAL;
						}
						*/
						int varLen = varStr.length();
						int start1 = 0, index1 = 0;
						int start2 = 0, end2 = 0, index2 = 0;
						while (index2 >= 0) {
							index2 = rn.value.indexOf(varStr, index2);
							if (index2 >= 0) {
								end2 = index2;
								index2 = index2 + varLen;
							} else {
								end2 = rn.value.length();
							}
							
							if (start2 > rn.value.length()) {
								start2 = rn.value.length();
							}
							String substr = rn.value.substring(start2, end2);
							index1 = child.value.indexOf(substr, start1);
							if ((index1 < 0) || (start2 == 0 && index1 != 0)) {
								rn.checkResult = CheckResult.NOTEQUAL;
								rn.actualValue = child.value;
								break;
							} else {
								start1 = index1 + substr.length();
							}
							start2 = index2;
						}
						
						if (rn.checkResult == CheckResult.NOTFOUND) {
							rn.checkResult = CheckResult.EQUAL;
						}
					} else if (rn.value.equals(child.value)) {
						rn.checkResult = CheckResult.EQUAL;
					} else {
						rn.checkResult = CheckResult.NOTEQUAL;
						rn.actualValue = child.value;
					}
				}
			} else {
				compNodeInTree(rn, child);
			}
		}

	}

	public TNode findSubTree(RNode rn, TNode root) {
		TNode child = null;
		TNode subTree = null;
		Iterator<TNode> it = root.subNodes.iterator();
		while (it.hasNext()) {
			child = it.next();

			if (!child.isLeaf()) {
				if (rn.key.equals(child.key)) {
					subTree = child;
				} else {
					subTree = findSubTree(rn, child);
				}
			}

			if (subTree != null) {
				break;
			}
		}

		return subTree;
	}

	public void compTrees(RNode rn, TNode root) {
		RNode child = null;
		TNode subTree = null;
		Iterator<RNode> it = rn.subNodes.iterator();
		while (it.hasNext()) {
			child = it.next();
			if (child.isLeaf()) {
				compNodeInTree(child, root);
			} else {
				subTree = findSubTree(child, root);
				if (subTree != null) {
					child.checkResult = CheckResult.EQUAL;
					compTrees(child, subTree);
				}

			}
		}
	}

	public void resetRTree(RNode root) {

		root.checkResult = CheckResult.NOTFOUND;
		root.actualValue = null;
		if (!root.isLeaf()) {
			RNode child = null;
			Iterator<RNode> it = root.subNodes.iterator();
			while (it.hasNext()) {
				child = it.next();
				resetRTree(child);
			}
		}

	}

	public boolean checkResult(RNode root) {
		boolean result = (root.checkResult == CheckResult.EQUAL);

		if (!result || root.isLeaf()) {
			return result;
		}

		if (!root.isLeaf()) {
			RNode child = null;
			Iterator<RNode> it = root.subNodes.iterator();
			while (it.hasNext()) {
				child = it.next();
				result = checkResult(child);
				if (!result) {
					break;
				}
			}
		}

		return result;
	}

	/*
	 * parseResultFile() By reading result file, parse it into three outputs: 
	 *  1) result prefix, whether it is E: Z: or 0:, into a StringBuilder, sbResultPrefix 
	 *  2) keywords, title of TRACE, such as Request_Generate_AMA, all into a String builder, sbKeywords 
	 *  3) Result trees, each line will be parsed as a tree, all are stored in an ArrayList
	 */
	public void parseResultFile(File rFile, StringBuilder sbResultPrefix,
			StringBuilder sbKeywords, ArrayList<RNode> resultTrees) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(rFile));
			String s = null;
			Pattern priority = Pattern.compile("^P[0-9]:");
			while ((s = br.readLine()) != null) {
				if (s.startsWith("#") || s.isEmpty()) {
					continue;
				}
				Matcher m = priority.matcher(s);
				if (m.lookingAt()) {
					s = s.substring(m.end());
				}
				
				int index = s.indexOf("(");
				int colon = s.indexOf(":");

				//if (colon < index && colon > 0) {
				if ((colon > 0) && (index < 0 || colon < index)) {
					sbResultPrefix.append(s.substring(0, colon + 1));
				} else {
					sbResultPrefix.append("Z:");
				}

				int start = colon + 1;
				if (colon > index) {
					start = 0;
				}

				if (index > 0) {
					sbKeywords.append(s.substring(start, index)).append("\n");
				} else {
					sbKeywords.append(s.substring(start)).append("\n");
				}

				RNode root = new RNode();
				genTree(root, s.substring(start));
				resultTrees.add(root);
			}

			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * parsePrefix() Parse result prefix from sbResultPrefix, check it whether a
	 * Character or a Number, Output is an Array
	 */
	public Object[] parsePrefix(StringBuilder sbResultPrefix) {
		String[] s = sbResultPrefix.toString().split(":");
		Pattern pDigit = Pattern.compile("[0-9]+");
		Pattern pZ = Pattern.compile("^[Z][0-9]+");
		Object[] obj = new Object[s.length];

		for (int i = 0; i < s.length; i++) {
			Matcher mDigit = pDigit.matcher(s[i]);
			Matcher mZ = pZ.matcher(s[i]);
			if (mDigit.matches()) {
				obj[i] = new Integer(Integer.parseInt(s[i]));
			} else if (mZ.matches()) {
				obj[i] = s[i];
			} else {
				obj[i] = new Character(s[i].charAt(0));
			}
		}

		return obj;
	}

	/*
	 * precessLog1() based on keywords from result file, get index of each
	 * keyword, put it into a StringBuilder array, sbIndex put all related TRACE
	 * into a string
	 */
	public String processLog1(File logFile, StringBuilder[] sbIndex,
			StringBuilder sbKeywords, BooleanClass hasSubscriptAt) {
		StringBuilder sbLog = new StringBuilder();
		String[] keywords = sbKeywords.toString().split("\n");
		boolean hasAMA = false;
		int AMAResultIndex = -1;
		for (int i = 0; i < keywords.length; i++) {
			if (isAMA(keywords[i])) {
				hasAMA = true;
				AMAResultIndex = i;
				break;
			}
		}

		try {
			BufferedReader br = new BufferedReader(new FileReader(logFile));

			String line = null;
			Pattern p = Pattern.compile("^ {3}");
			while ((line = br.readLine()) != null) {
				if (line.startsWith("   at_line:")) {
					continue;
				}
				
				if (line.contains("subscript at")) {
					if (hasSubscriptAt != null) {
						hasSubscriptAt.flag = true;
					}					
					continue;
				}

				String s = null;
				Matcher m = p.matcher(line);
				if (m.find()) {
					s = line.replaceFirst("   ", "");
				} else {
					s = line;
				}

				if (hasAMA) {
					Matcher matchAMA = AMA_PATTERN.matcher(s);
					if (matchAMA.find()
							&& (s.charAt(matchAMA.start() - 1) == ' ')
							&& (s.charAt(matchAMA.end()) == '(')) {
						int pos = matchAMA.start();
						int index = sbLog.length() + pos;
						sbIndex[AMAResultIndex].append(",").append(index);
						sbLog.append(s);
						continue;
					}
				}

				for (int i = 0; i < keywords.length; i++) {
					if (s.contains(keywords[i] + "(")) {
						int pos = s.indexOf(keywords[i] + "(");
						if (!s.startsWith(keywords[i]) && (s.charAt(pos - 1) != ' ')) {
							continue;
						}

						int index = sbLog.length() + pos;
						sbIndex[i].append(",").append(index);

						break; // to avoid duplicate parse in case there are
								// more than one same keyword in result file
					}
				}
				sbLog.append(s);
			}

			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return sbLog.toString();
	}

	/*
	 * Generate trace Trees from trace string
	 */
	public ArrayList<TNode> processLog2(String strLog, StringBuilder[] sbIndex)
			throws CantParseException {
		ArrayList<TNode> traceTrees = new ArrayList<TNode>();
		for (int i = 0; i < sbIndex.length; i++) {
			String[] idx = sbIndex[i].toString().split(",");
			for (int j = 1; j < idx.length; j++) {
				int start = Integer.parseInt(idx[j]);
				int end = findEndBracket(strLog, start) + 1;
				TNode root = new TNode();
				try {
					genTree(root, strLog.substring(start, end));
				} catch (StringIndexOutOfBoundsException e) {
					throw (new CantParseException());
				}
				
				traceTrees.add(root);
			}
		}

		return traceTrees;
	}

	public boolean compareLists(ArrayList<RNode> resultTrees,
			ArrayList<TNode> traceTrees, Object[] resultPrefix) {
		boolean result = true;
		for (int i = 0; i < resultTrees.size(); i++) {
			RNode rn = resultTrees.get(i);
			
			if (resultPrefix[i] instanceof Integer) {
				int pos = ((Integer) resultPrefix[i]).intValue();
				int index = 0;
				for (int j = 0; j < traceTrees.size(); j++) {
					TNode tn = traceTrees.get(j);
					if (rn.key.equals(tn.key)) {
						if (index == pos) {
							rn.checkResult = CheckResult.EQUAL;
							compTrees(rn, tn);
							
							break;
						} else {
							index++;
						}
					} else if (isAMA(rn.key) && isAMA(tn.key)) {
						index++;
					}
				}
				
				if (!checkResult(rn)) {
					result = false;
				}
				
			} else if (resultPrefix[i] instanceof String) {
				Pattern p = Pattern.compile("Z[0-9]+");
				String prefix = (String) resultPrefix[i];
				Matcher m = p.matcher(prefix);
				if (m.matches()) {
					System.out.println("got Z + digits!");
					int total = Integer.parseInt(prefix.substring(1));
					int number = 0;
					for (int j = 0; j < traceTrees.size(); j++) {
						TNode tn = traceTrees.get(j);
						if (rn.key.equals(tn.key)) {
							if (rn.checkResult != CheckResult.NOTFOUND) {
								//resetRTree(rn);
								rn.reset();
							}
							
							rn.checkResult = CheckResult.EQUAL;
							compTrees(rn, tn);
							
							if (checkResult(rn)) {
								number++;
								/* what if number > total? keep searching? 
								 * maybe update in the future
								if (number == total) {
									break;
								}
								*/
							}
						}
					}
					if (number >= total) {
					// there might another non-equal trace is compared, which results rn's result is NOTEQUAL
						rn.setToEqual();
					}
					resultPrefix[i] = prefix + "-" + number;
					if (number != total) {
						result = false;
					}
				}
			} else {
				char ch = ((Character) resultPrefix[i]).charValue();
				if (ch == 'Z' || ch == 'E') {
					for (int j = 0; j < traceTrees.size(); j++) {
						TNode tn = traceTrees.get(j);
						if (rn.key.equals(tn.key)) {

							if (rn.checkResult != CheckResult.NOTFOUND) {
								//resetRTree(rn);
								rn.reset();
							}

							rn.checkResult = CheckResult.EQUAL;
							compTrees(rn, tn);

							if (checkResult(rn)) {
								break;
							}
						}
					}
					
					if ((ch == 'Z' && !checkResult(rn))
							|| (ch == 'E') && checkResult(rn)) {
						result = false;
					}
				}
			}
		}
		
		return result;
	}
	
	public void moveToDir(String file, String destDir) {
		File destDirectory = new File(destDir);
		File origFile = new File(file);
		if (!destDirectory.exists()) {
			destDirectory.mkdir();
		}
		
		if (!origFile.exists()) {
			return;
		}
		
		origFile.renameTo(new File(destDirectory.getAbsoluteFile(), origFile.getName()));
	}
	
	public boolean parseCase(Case c) throws CantParseException {
		String basedir = controller.getBaseDir();
		StringBuilder relDir = new StringBuilder(basedir);
		relDir.append("/").append(c.getCustomer()).append("/")
				.append(c.getRelease());
		String resultFile = relDir.toString() + "/res/" + c.getTID() + ".result";
		String logFile = relDir.toString() + "/log/" + c.getTID() + ".log";
		
		File rFile = new File(resultFile);
		File lFile = new File(logFile);
		
		boolean result = compareLog(lFile, rFile);
		if (!result) {
			String faillogDir = relDir.toString() + "/faillog";
			String parseResult = relDir.toString() + "/log/" + c.getTID() + ".FAIL";
			moveToDir(lFile.getAbsolutePath(), faillogDir);
			moveToDir(parseResult, faillogDir);
			File previousPassResult = new File(relDir.toString() + "/log/" + c.getTID() + ".PASS");
			if (previousPassResult.exists()) {
				previousPassResult.delete();
			}
		} else {
			File previousFaillog = new File(relDir.toString() + "/faillog/" + c.getTID() + ".log");
			File previousFailResult = new File(relDir.toString() + "/faillog/" + c.getTID() + ".FAIL");
			if (previousFaillog.exists()) {
				previousFaillog.delete();
			}
			if (previousFailResult.exists()) {
				previousFailResult.delete();
			}
		}
		
		return result;
	}
	
	public boolean reParseCase(Case caseToParse, boolean originalResult) throws CantParseException {
		String basedir = controller.getBaseDir();
		StringBuilder relDir = new StringBuilder(basedir);
		relDir.append("/").append(caseToParse.getCustomer()).append("/")
				.append(caseToParse.getRelease());
		String logDir = originalResult ? "/log/" : "/faillog/";
		String resultFile = relDir.toString() + "/res/" + caseToParse.getTID() + ".result";
		String logFile = relDir.toString() + logDir + caseToParse.getTID() + ".log";
		
		File rFile = new File(resultFile);
		File lFile = new File(logFile);
		
		boolean result = compareLog(lFile, rFile);
		if (result != originalResult) {
			String origSuffix = originalResult ? ".PASS" : ".FAIL";
			File origParseResult = new File(lFile.getParent() + "/" 
					+ caseToParse.getTID() + origSuffix);
			if (origParseResult.exists()) {
				origParseResult.delete();
			}			
			
			String destDir = result ? "/log/" : "/faillog/";
			String suffix = result ? ".PASS" : ".FAIL";
			String newParseResult = relDir.toString() + logDir 
					+ caseToParse.getTID() + suffix;
			moveToDir(lFile.getAbsolutePath(), relDir.toString() + destDir);
			moveToDir(newParseResult, relDir.toString() + destDir);			
		}
		
		return result;
	}
	
	public boolean compareLog(File logFile, File resultFile) throws CantParseException {
		/* Get the directory of log file */
		boolean result;
		BooleanClass hasSubscriptAt = new BooleanClass(false);
		String logDir = logFile.getParent();
		String logName = logFile.getName();
		String tid = logName.substring(0, logName.indexOf("."));

		if (!(resultFile.exists() && logFile.exists())) {
			throw (new CantParseException());
		}

		StringBuilder sbResultPrefix = new StringBuilder();
		StringBuilder sbKeywords = new StringBuilder();
		ArrayList<RNode> resultTrees = new ArrayList<RNode>();
		parseResultFile(resultFile, sbResultPrefix, sbKeywords, resultTrees);

		Object[] resultPrefix = parsePrefix(sbResultPrefix);

		StringBuilder[] sbIndex = new StringBuilder[resultTrees.size()];
		for (int i = 0; i < sbIndex.length; i++) {
			sbIndex[i] = new StringBuilder("-1");
		}
		String strLog = processLog1(logFile, sbIndex, sbKeywords, hasSubscriptAt);

		ArrayList<TNode> traceTrees;
		
		traceTrees = processLog2(strLog, sbIndex);		

		result = compareLists(resultTrees, traceTrees, resultPrefix);
		
		if (hasSubscriptAt.flag) {
			result = false;
		}

		String suffix = result ? ".PASS" : ".FAIL";
		String parseResult = logDir + "/" + tid + suffix;
		try {
			PrintWriter parseWriter = new PrintWriter(
					new BufferedWriter(
							new FileWriter(parseResult)));
			
			if (hasSubscriptAt.flag) {
				parseWriter.println("FAIL: \"subscript at\" exists in log\n");
			}
			
			for (int i = 0; i < resultTrees.size(); i++) {
				parseWriter.println(resultPrefix[i] + ":" + resultTrees.get(i).toTreeString(0));
			}
			
			parseWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//LogParser lp = new LogParser();
		XController controller = new XController();
		controller.setBaseDir("D:\\automation\\R29SUH");
		LogParser lp = controller.getLogParser();
		Case c = new Case("de5408", "72400", "R27SU7", "VFCZ", "Audit", "VFCZ");
		//boolean result = lp.parseCase(c);
		boolean result = false;
		try {
			result = lp.reParseCase(c, true);
		} catch (CantParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("result = " + result);

	}

}
