import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlToXhtml {
	// Regex-formatted list of known HTML self-closing element types.
	private static final String SELF_CLOSING_TAGS = "area|base|br|col|embed|"
			+ "hr|img|input|link|meta|param|source|track|wbr|command|keygen|menuitem";

	// Regex-formatted list of known attributes that are valid for attribute minimization.
	private static final String MINIMIZABLE_ATTRIBUTES = "compact|checked|declare|readonly|disabled|"
			+ "selected|defer|ismap|nohref|noshade|nowrap|multiple|noresize";


	// Main function.
	public static void main(String[] args) {
		if(args.length == 0) {
			System.out.println("Please enter a valid file as a parameter to this application.");
			System.exit(1);
		}
		// Read the input file location from the first command-line argument.
		File inputHtmlFile = new File(args[0]);
		// Basic sanity check: does the file exist?
		if(!inputHtmlFile.exists() || inputHtmlFile.isDirectory()) {
			System.out.println("Input raw HTML file is not valid!");
			System.exit(1);
		}
		// Put together the file's content into a string, line-by-line.
		StringBuilder fileContent = new StringBuilder();
		try {
			// Pretty self-explanatory.
			BufferedReader inputReader = new BufferedReader(new FileReader(inputHtmlFile));
			String inputLine = "";
			while((inputLine = inputReader.readLine()) != null) {
				fileContent.append(inputLine);
				// DEBUG
				System.out.println(inputLine);
			}
			inputReader.close();
		} catch (Exception e) {
			// Generic exception-handler to write out the trace and exit.
			e.printStackTrace();
			System.exit(1);
		}
		// Begin building the header, noting that TITLE is a required HEAD item.
		StringBuilder newBody = new StringBuilder();
		String htmlContent = fileContent.toString();
		// Assuming the XML, DOCTYPE, and html tag attributes here, please change them as needed.
		newBody
			.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
			.append("\n<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">")
			.append("\n<html xmlns=\"http://www.w3.org/TR/xhtml1\" xml:lang=\"en\" lang=\"en\">\n<head>");
		// Check if HEAD content is already defined -- if so, add it; if not create one.
		// The HEAD element is INTENTIONALLY NOT CLOSED here.
		if(htmlContent.contains("<head>")) {
			String headContent = "\n" + htmlContent.substring(
				htmlContent.indexOf("<head>") + 6, htmlContent.indexOf("</head>"));
			// Forcibly close self-terminating tags (like 'meta' and 'link').
			headContent = headContent
					.replaceAll("(>)?\\s+<", "$1\n<")
					.replaceAll("(?i)<\\s*(" + SELF_CLOSING_TAGS + ")(\\s*[^>]*?[^/])?>", "<$1$2 />")
					.replaceAll("<([^>]+)>", "<\\L$1>"); //lower-case all HEAD tags and attributes
			newBody.append(headContent);
		} else { newBody.append("<head>"); }
		// Check for the REQUIRED TITLE element in the HEAD section.
		if(!newBody.toString().contains("<title>")) {
			// A title doesn't exist, time to add a default one so generic it will have to be changed.
			newBody.append("<title>File Converted by HTML-to-XHTML Java Conversion Tool</title>");
		}
		// Close out the HEAD element now.
		newBody.append("\n</head>");
		// And finally, correct the body's HTML.
		newBody.append("\n<body>" + htmlToPrint(htmlContent) + "\n</body>\n</html>");

		// Can add something here besides just outputting the result, like writing to a corrected .xhtml file.
		System.out.println(newBody.toString()); // THIS IS THE PRETTY-PRINT VERSION
		System.out.println("\n\nMinimized XHTML:\n" + stripPrettyPrint(newBody.toString()));
		System.exit(0);
	}

	/**
	 * Accept raw HTML input and return pretty-printed XHTML output.
	 *
	 */
	private static String htmlToPrint(String inputContent) {
		// Set the local variable to the document text with all line breaks removed.
		String newDoc = inputContent.replaceAll("\\R+", " ");
		// Immediately replace any empty Definition Lists "dl", or UL/OL tags.
		newDoc = newDoc.replaceAll("<\\s*([duo]l)\\s*[^>]*?>[\\s]*<\\s*\\/([duo]l)\\s*[^>]*?>", "");
		// Force any "body" tag to be lower-case.
		newDoc = newDoc.replaceAll("(?i)<(\\/?)body>", "<$1body>");
		String xhtmlBody = "";

		if(newDoc != null && newDoc.contains("<body>")) {
			StringBuilder xhtmlCorrected = new StringBuilder();
			// Fetch only the content between the <body> element.
			newDoc = newDoc.substring(newDoc.indexOf("<body>") + 6, newDoc.indexOf("</body>"));

			System.out.println("INPUT TEXT : " + newDoc);

			// List all tags, closing tags, and self-closing tags in the document body (and their indices).
			//     Index the map by the start index of the substring (which will always be unique).
			LinkedHashMap<Integer, String> tagCollection = new LinkedHashMap<>();
			LinkedHashMap<Integer, String> tagContents = new LinkedHashMap<>();
			String trailingText = getHtmlTags(tagCollection, tagContents, newDoc);

			// This section breaks apart attributes and corrects them per-tag.
			for(Integer pos : tagCollection.keySet()) {
				String tag = tagCollection.get(pos);
				System.out.println("Examining position " + pos + " of HTML; tag: " + tag);

				// If (for some reason) any whitespace precedes the element name, remove it.
				tag = tag.replaceAll("<\\s+", "<");
				// Same for the closing end of the tag.
				tag = tag.replaceAll("\\s+(\\s/)?>", "$1>");
				// Remove any tag namespace prefixes.
				tag = tag.replaceAll("<\\w+:([^\\s]+)", "<$1");
				// Quote unquoted attribute values, e.g. (class=test) --> (class="test")
				tag = tag.replaceAll("\\s+(\\w+=)([^\"'][^\\s>]+)", " $1\"$2\"");
				// Expand minimized attributes.
				tag = tag.replaceAll("(" + MINIMIZABLE_ATTRIBUTES + ")([^=\"'])", "$1=\"$1\"$2");

				System.out.println("SANITIZED LINE : " + tag);

				// Attempt to find the element type from the tag (no attributes or angle brackets).
				Matcher matchedElementType = Pattern.compile("<\\s*(/?\\w+).*?>").matcher(tag);
				// If it wasn't found (meaning invalid tag), move to the next tag in the list.
				if(!matchedElementType.find()) {
					continue;
				}
				// Set the element type to the first match for the first capture group.
				String elementType = matchedElementType.group(1);
				// Get and sanitize the attributes of the element.
				ArrayList<String> tagAttributes = new ArrayList<>();
				Matcher matchedAttributes =
					Pattern.compile("\\s+(\\w+)(=[^\\s].+?)(?:([\"'])|\\/?>)").matcher(tag);
				StringBuilder styleContents = new StringBuilder();
				while(matchedAttributes.find()) {
					/* FIXUP to collect all inline STYLE information that already exists and aggregate
					      it with any tag/attribute replacements, e.g. bgcolor --> background-color */
					// Simultaneously extract the single attribute and convert the attribute name to lower case.
					if ( matchedAttributes.group( 1 ).equalsIgnoreCase( "style" ) ) {
						// If the attribute name is 'style', append to the current style contents variable.
						String styleIdentity = matchedAttributes.group( 2 ) != null ? matchedAttributes.group( 2 ) : "";
						styleIdentity = styleIdentity.substring( 1 ).replaceAll( "['\"]+", "" );
						styleContents.append( styleIdentity + (styleIdentity.endsWith( ";" ) ? "" : ";") );
						continue;
					} else if ( matchedAttributes.group( 1 ).equalsIgnoreCase( "bgcolor" ) ) {
						// If the attribute name is 'bgcolor', drop the attribute and add 'background-color' to the style contents.
						//  This is permissible to duplicate, but can lead to conflicting colors.
						String colorIdentity = matchedAttributes.group( 2 ) != null ? matchedAttributes.group( 2 ) : "";
						colorIdentity = colorIdentity.substring( 1 ).replaceAll( "['\"]+", "" );
						styleContents.append( "background-color:" + colorIdentity + ";" );
						continue;
					}
					/* END FIXUP */
					// Simultaneously extract the single attribute and convert the attribute name to lower case.
					StringBuilder attribute = new StringBuilder();
					attribute.append(matchedAttributes.group(1).toLowerCase());
					attribute.append(matchedAttributes.group(2) != null ? matchedAttributes.group(2) : "");
					attribute.append(matchedAttributes.group(3) != null ? matchedAttributes.group(3) : "");
					System.out.println("    ATTRIBUTE : " + attribute.toString());
					// Add the finished attribute to the final tagAttributes list object.
					tagAttributes.add(attribute.toString());
				}

				// Create the finalized style tagging.
				String styleTagFinal = styleContents.length() > 0
					? " style=\"" + styleContents.toString() + "\"" : null;

				// Build the full corrected tag to append to the final XHTML body.
				StringBuilder xhtmlTag = new StringBuilder();
				xhtmlTag.append("<" + elementType.toLowerCase());
				for(String attrib : tagAttributes) {
					xhtmlTag.append(" " + attrib);
				}
				// Append the final style information, if defined, then the closing angle bracket.
				if ( styleTagFinal != null ) {
					xhtmlTag.append( styleTagFinal );
				}
				xhtmlTag.append(">");

				// Forcibly close the tag if it's a self-terminating type.
				String xhtmlTagAsString = xhtmlTag.toString();
				xhtmlTagAsString = xhtmlTagAsString.replaceAll(
						"(?i)<\\s*(" + SELF_CLOSING_TAGS + ")(\\s*[^>]*?[^/])?>", "<$1$2 />");
				System.out.println("    FINAL TAG : " + xhtmlTagAsString);

				// Append the new content to the intermediate object.
				if(tagContents.get(pos) != null) {
					// This section consists of the previous "between-tags" (nested) content with
					//     the corrected XHTML tag at the end.
					xhtmlCorrected.append(tagContents.get(pos) + xhtmlTagAsString);
				}
			}

			// Clean up any special characters.
			String correctedHtml = xhtmlCorrected.toString() + trailingText;
			correctedHtml = correctedHtml.replace( "&reg;", "®" ).replace( "&copy;", "©" );
			// Add a non-breaking space behind the line-break to force their acknowledgement.
			correctedHtml = correctedHtml.replaceAll("(?i)<br", "&#160;<br");

			// Now fix the tag nesting, using the same process as above to read back in the correctedHtml.
			//   Reusing variables here doesn't hurt anything.
			tagCollection = new LinkedHashMap<>();
			tagContents = new LinkedHashMap<>();
			// Pass in the maps to repopulate them, and assign all leftover content to this String.
			trailingText = getHtmlTags(tagCollection, tagContents, correctedHtml);
			StringBuilder finalXhtml = new StringBuilder();

			// Set up a tagStack as a pseudo-stack to track opening tags,
			//     which are 'popped' as the corresponding closing tags are encountered.
			ArrayList<String> tagStack = new ArrayList<>();

			// Spacing StringBuilder to control a little bit of pretty-printing.
			StringBuilder contentIndent = new StringBuilder();
			// Append an initial spacing because this content is nested inside of the <body> tag anyhow.
			contentIndent.append("  ");
			// Iterate through each tag and track the DOM nesting level, inserting closing tags as needed.
			//   This section will collapse a section where there are mismatched tags and rearrange
			//   the nesting appropriately to create valid tag layering.
			for(Integer pos : tagCollection.keySet()) {
				String tag = tagCollection.get(pos);
				StringBuilder modifiedTag = new StringBuilder();
				// Attempt to find the element type from the tag (no attributes or angle brackets).
				Matcher matchedElementType = Pattern.compile("<\\s*(/?\\w+)[^>]*?>").matcher(tag);
				// If it wasn't found (meaning invalid tag), move to the next tag in the list.
				if(!matchedElementType.find()) {
					continue;
				}
				// Set the element type to the first match for the first capture group.
				String elementType = matchedElementType.group(1);

				// Interpret whether or not the tag is a closing tag.
				boolean isClosingTag = elementType.matches("^/\\w+$");
				// Check if this is a self-closing tag.
				boolean isSelfClosing = tag.matches("<(" + SELF_CLOSING_TAGS + ").+?/>");

				// Take an action based on the type of tag being parsed.
				if(!isClosingTag && !isSelfClosing) {
					// Tag is an "opening" tag but isn't self-terminating; add it to the stack.
					//   If it's an LI tag, check the outer wrapping tag to make sure it's not an orphan.
					if(!tagStack.isEmpty()) {
						if(elementType.equalsIgnoreCase("li") && !(
							tagStack.get(tagStack.size()-1).equalsIgnoreCase("ul")
							|| tagStack.get(tagStack.size()-1).equalsIgnoreCase("ol")
						)) {
							// If the LI tag isn't wrapped by one of the two list types, assume an UL wrapper.
							//   This seems to be most HTML clients' default behavior.
							tagStack.add("ul");
							modifiedTag.append("<ul>");
						} else if(!elementType.equalsIgnoreCase("li") && (
							tagStack.get(tagStack.size()-1).equalsIgnoreCase("ul")
							|| tagStack.get(tagStack.size()-1).equalsIgnoreCase("ol")
						)) {
							// If the parent tag is an UL or OL element, but the next opening tag in line
							//   isn't, add an LI tag in the middle by force. It will be auto-closed later.
							tagStack.add("li");
							modifiedTag.append("<li>");
						}
					} else if(tagStack.isEmpty() && elementType.equalsIgnoreCase("li")) {
						// Cases where LI is encountered at the root of a document.
						tagStack.add("ul");
						modifiedTag.append("<ul>");
					}
					tagStack.add(elementType);
					// ... and also add it to the final tag output (without any modification) for the XHTML.
					modifiedTag.append(tag);
				} else if(isClosingTag && !isSelfClosing) {
					// Intentionally declared in a higher scope. See the 'if' below.
					int i = 0;
					// Prevent cascading pops when there are consecutive tags of the same type in the stack.
					boolean isElementPopped = false;
					// 'i' starts at 1 and moves to the stack size...
					for(i = 1; i <= tagStack.size(); i++) {
						if(isElementPopped) {
							continue;
						}
						// ... because the tags are retrieved from the top of the stack going down it.
						// Note that elementType.substring(1) is simply removing the leading '/' character from the tag.
						if(tagStack.get(tagStack.size() - i).equals(elementType.substring(1))) {
							// DEBUG.
							System.out.println("CLOSE: " + elementType.substring(1) + " - STACK POS (from top):  " + i);
							// If the matching opening tag is further down than the stack's top...
							if(i > 1) {
								// ... there are other tags further up the stack. Close them in a cascading manner.
								// sizeLimit : the top boundary of the stack (should never be accessed directly or will throw out of bounds ex).
								int sizeLimit = tagStack.size();
								// startIndex : the location from the top of the stack where the
								int startIndex = tagStack.size() - i;
								// Start at the top of the stack, run down all the way to the matched opening tag.
								// This operation will NOT pop any variables from the array, so that the size stays unchanged.
								for(int j = sizeLimit - 1; j > startIndex; j--) {
									// Append the closing form of the tag from the stack at position j.
									modifiedTag.append("</" + tagStack.get(j) + ">");
									// Control the StringBuilder for indentation.
									if(contentIndent.length() >= 2) {
										contentIndent.delete(0, 2);
									}
								}
								// Finally, append the actual closing tag being examined.
								modifiedTag.append(tag);
								// Start from position 'x' inside the stack, and pop out everything above it.
								for(int x = startIndex; x < sizeLimit; x++) {
									// This isn't a removal on 'x' because as it pops variables out of the array
									//     the size will shrink dynamically. So hold it at a fixed constant and
									//     just delete the content from that point in the stack up to the top.
									//     e.g. 1 2 (3) 4 5, where 3 = startIndex. Removing items will shift 4 and 5
									//     down to 3 and 4 respectively ad infinitum until the stack is cleaned upward.
									tagStack.remove(startIndex);
								}
								isElementPopped = true;
							} else {
								// Should be only popping that item from the top of the stack.
								tagStack.remove(tagStack.size() - i);
								// There's nothing to get from the stack, so append the tag and be done.
								modifiedTag.append(tag);
								isElementPopped = true;
							}
						}
					}
					if(i == tagStack.size()) {
						// The closing tag for this element isn't on the list.
						// Nothing is needed here since there's no append method call.
						// Kept as a placeholder in case it's needed.
						continue;
					}
				} else if(isSelfClosing) {
					// The tag closes itself, no stack-based operation is needed. Append to final output.
					modifiedTag.append(tag);
				}

				// DEBUG
				System.out.println("MOD TAG : " + modifiedTag.toString() + " - POS : " + pos);
				// Pretty-print attempt, delete the indentation BEFORE outputting the closing tag.
				if(!isSelfClosing && isClosingTag && !modifiedTag.toString().equals("")) {
					if(contentIndent.length() >= 2) {
                        contentIndent.delete(0, 2);
                    }
				}

				System.out.println("\n\n\n~~~ TAG: " + tag + "\n~~~~~ Contents: " + tagContents.get(pos)
					+ "\n~~~~~ ELEMENT: " + elementType + "\n~~~~~ MOD TAG: " + modifiedTag.toString());

				// Add the new content onto the final XHTML body, provided the tagContents could be retrieved
				//     and provided the final element is not just a whitespace item.
				if(tagContents.get(pos) != null && !modifiedTag.toString().equals("")) {
					if(!tagContents.get(pos).equals("") && !tagContents.get(pos).matches("\\s+")) {
						if(elementType.equalsIgnoreCase("li") || elementType.matches("(?i)\\/[uo]l")) {
							// If this was a LIST item opening, or a List TAG >>CLOSING<<,
							//   check the tagContents for intermediate content. If some exists,
							//   wrap it in LI tags as well.
							finalXhtml.append("\n  " + contentIndent.toString() +
												"<li>" + tagContents.get(pos) + "</li>");
							System.out.println("-- Added <(li|/ul|/ol)>:   " + tagContents.get(pos));
						} else {
							// Since the tagContents key actually has something in it, add it w/o modification.
							finalXhtml.append("\n  " + contentIndent.toString() + tagContents.get(pos));
						}
					}
					finalXhtml.append("\n" + contentIndent.toString() + modifiedTag.toString());
				}
				// Pretty-print attempt, add the indentation AFTER outputting the opening tag.
				if(!isSelfClosing && !isClosingTag && !modifiedTag.toString().equals("")) {
					contentIndent.append("  ");
				}
			}

			// Add on the trailing text that doesn't have tags.
			finalXhtml.append("\n" + contentIndent + trailingText);

			// Go through the tags remaining on the stack and close them sequentially.
			//     These will be added AFTER the trailing text above.
			for(int y = tagStack.size() - 1; y >= 0; y--) {
				if (contentIndent.length() >= 2) {
					contentIndent.delete(contentIndent.length() - 2, contentIndent.length());
				}
				finalXhtml.append("\n" + contentIndent + "</" + tagStack.get(y) + ">");
			}

			// Final StringBuilder conversion for the return variable.
			xhtmlBody = finalXhtml.toString();

		} else {
			// Return some default "error" text. Shouldn't ever happen.
			xhtmlBody = "<p>Unable to convert this document to XHTML.</p>";
		}

		// Return the finished XHTML.
		return xhtmlBody;
	}


	// Populate the first two parameters (passed as reference), using the third parameter.
	public static String getHtmlTags(LinkedHashMap<Integer, String> collection,
									 LinkedHashMap<Integer, String> contents, String inputHTML) {
		// Effectively 'grep' all tags out of the provided input string (raw HTML).
		Matcher m = Pattern.compile("(<\\s*/?\\w+[^>]*?>)").matcher(inputHTML);
		// Set up the initial previousTagLocation as the very base of the string.
		int previousTagLocation = 0;
		// While there are more matches to iterate...
		while(m.find()) {
			// Record the matched tag, using the always-unique start index within the raw HTML as the key.
			collection.put((Integer)m.start(), m.group(1));
			// The previous tag location to the start of the current tag is considered this location's "content".
			// NOTE: a substring with matching start and end locations will NOT throw an exception.
			contents.put((Integer)m.start(), inputHTML.substring(previousTagLocation, m.start()));
			// Set the previous tag location to the start of the content AFTER the matched tag.
			previousTagLocation = m.start() + m.group(1).length();
		}
		// Though the maps are populated, also return the last bit of content that is NOT wrapped by anything.
		return inputHTML.substring(previousTagLocation);
	}


	// Get rid of the cutesy pretty-print because it can really mess with some of the spacing in HTML documents.
	private static String stripPrettyPrint(String content) {
		return content.replaceAll("\\R+\\s*", "");
	}
}
