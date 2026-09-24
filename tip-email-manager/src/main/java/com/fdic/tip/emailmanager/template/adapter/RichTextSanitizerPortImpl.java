package com.fdic.tip.emailmanager.template.adapter;

import com.fdic.tip.emailmanager.common.constants.RichTextSanitizerConstants;
import com.fdic.tip.emailmanager.template.service.RichTextSanitizerPort;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Safelist;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Component;

/**
 * Reduces pasted/composed rich text down to the toolbar's supported
 * formatting set (EM-8 AC: "authors cannot inject unsupported markup by
 * pasting from other applications... structure preserved, unsupported
 * styling discarded") and derives the plain-text alternative EM-8 also
 * requires (headings/lists readable, links show their address, images
 * become short placeholders).
 *
 * Maven: org.owasp.html:owasp-java-html-sanitizer, org.jsoup:jsoup
 */
@Component
public class RichTextSanitizerPortImpl implements RichTextSanitizerPort {

    // Toolbar = headings, bold/italic/underline, ordered/bulleted lists,
    // links, block quotes, tables, inline images (EM-8 AC list). Nothing
    // outside this set survives sanitization, however it arrived.
    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            .allowCommonInlineFormattingElements()   // b, i, u, em, strong, etc.
            .allowElements("h1", "h2", "h3", "blockquote", "p", "br", "div")
            .allowElements("ul", "ol", "li")
            .allowStandardUrlProtocols()
            .allowElements("a")
            .allowAttributes("href").onElements("a")
            .allowAttributes("target").matching(false, "_blank").onElements("a")
            .allowElements("table", "thead", "tbody", "tr", "th", "td")
            .allowElements("img")
            .allowAttributes("src", "alt", "width", "height").onElements("img")
            .toFactory()
            .and(Sanitizers.LINKS);

    @Override
    public String sanitize(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) {
            return "";
        }
        return POLICY.sanitize(rawHtml);
    }

    @Override
    public String toPlainText(String sanitizedHtml) {
        if (sanitizedHtml == null || sanitizedHtml.isBlank()) {
            return "";
        }
        // Parsed with a permissive Safelist purely for traversal — the
        // HTML was already reduced to the supported tag set above, so
        // this step is only ever converting known-good markup to text.
        Document doc = Jsoup.parse(Jsoup.clean(sanitizedHtml, Safelist.relaxed()
                .addTags("h1", "h2", "h3")));

        StringBuilder out = new StringBuilder();
        renderNode(doc.body(), out);

        return out.toString()
                .replaceAll("[ \\t]+\\n", "\n")   // trailing spaces before a break
                .replaceAll("\\n{3,}", "\n\n")     // collapse runs of blank lines
                .trim();
    }

    private void renderNode(Node node, StringBuilder out) {
        for (Node child : node.childNodes()) {
            if (child instanceof TextNode textNode) {
                out.append(textNode.text());
                continue;
            }
            if (!(child instanceof Element el)) {
                continue;
            }
            switch (el.tagName()) {
                case "br" -> out.append('\n');
                case "h1", "h2", "h3", "p", "div", "blockquote" -> {
                    renderNode(el, out);
                    out.append("\n\n");
                }
                case "li" -> {
                    out.append(RichTextSanitizerConstants.PLAIN_TEXT_LIST_BULLET);
                    renderNode(el, out);
                    out.append('\n');
                }
                case "ul", "ol" -> {
                    renderNode(el, out);
                    out.append('\n');
                }
                case "a" -> {
                    String href = el.attr("href");
                    renderNode(el, out);
                    if (!href.isBlank()) {
                        out.append(" (").append(href).append(")");
                    }
                }
                case "img" -> {
                    String label = !el.attr("alt").isBlank() ? el.attr("alt") : RichTextSanitizerConstants.PLAIN_TEXT_UNNAMED_IMAGE;
                    out.append(RichTextSanitizerConstants.PLAIN_TEXT_IMAGE_PLACEHOLDER_PREFIX)
                            .append(label)
                            .append(RichTextSanitizerConstants.PLAIN_TEXT_IMAGE_PLACEHOLDER_SUFFIX);
                }
                case "table" -> {
                    renderTable(el, out);
                    out.append('\n');
                }
                default -> renderNode(el, out);
            }
        }
    }

    private void renderTable(Element table, StringBuilder out) {
        for (Element row : table.select("tr")) {
            StringBuilder line = new StringBuilder();
            for (Element cell : row.select("th, td")) {
                if (line.length() > 0) {
                    line.append('\t');
                }
                line.append(cell.text());
            }
            out.append(line).append('\n');
        }
    }
}
