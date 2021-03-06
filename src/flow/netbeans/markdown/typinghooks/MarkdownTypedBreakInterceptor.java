package flow.netbeans.markdown.typinghooks;

import flow.netbeans.markdown.csl.MarkdownLanguageConfig;
import flow.netbeans.markdown.options.MarkdownGlobalOptions;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.spi.editor.typinghooks.TypedBreakInterceptor;
import org.openide.text.NbDocument;

/**
 *
 * @author junichi11
 */
public class MarkdownTypedBreakInterceptor implements TypedBreakInterceptor {

    private static final String INSERT_REGEX = "^((?<indent>\t+|\\s+)*((?<list>[-*+>] |(?<number>\\d+)\\. ).+|.*))$"; // NOI18N
    private static final String AFTER_INSERT_REGEX = "^(?<indent>\t+|\\s+)*(?<list>[-*+>] |(?<number>\\s\\d+)\\. )$"; // NOI18N
    private static final String LIST_GROUP = "list"; // NOI18N
    private static final String NUMBER_GROUP = "number"; // NOI18N
    private static final String INDENT_GROUP = "indent"; // NOI18N
    private static final Pattern INSERT_PATTERN = Pattern.compile(INSERT_REGEX);
    private static final Pattern AFTER_INSERT_PATTERN = Pattern.compile(AFTER_INSERT_REGEX);

    @Override
    public boolean beforeInsert(Context context) throws BadLocationException {
        return false;
    }

    @Override
    public void insert(MutableContext context) throws BadLocationException {
        if (!isEnabled()) {
            return;
        }

        if (!isAutoAdditionList()) {
            return;
        }

        StyledDocument document = (StyledDocument) context.getDocument();
        int caretOffset = context.getCaretOffset();
        int lineNumber = NbDocument.findLineNumber(document, caretOffset);
        int lineOffset = NbDocument.findLineOffset(document, lineNumber);
        String lineText = document.getText(lineOffset, caretOffset - lineOffset);

        StringBuilder sb = new StringBuilder("\n"); // NOI18N
        Matcher matcher = INSERT_PATTERN.matcher(lineText);
        if (matcher.find()) {
            String numberText = matcher.group(NUMBER_GROUP);
            String hlText = matcher.group(LIST_GROUP);
            String indentText = matcher.group(INDENT_GROUP);
            if (indentText != null) {
                sb.append(indentText);
            }

            if (numberText != null) {
                int number = Integer.parseInt(numberText);
                sb.append(++number).append(". "); // NOI18N
            } else if (hlText != null) {
                sb.append(hlText);
            }
        }
        context.setText(sb.toString(), 0, sb.length());
    }

    @Override
    public void afterInsert(Context context) throws BadLocationException {
        if (!isEnabled()) {
            return;
        }

        final StyledDocument document = (StyledDocument) context.getDocument();
        int caretOffset = context.getCaretOffset();
        int lineNumber = NbDocument.findLineNumber(document, caretOffset);
        int lineOffset = NbDocument.findLineOffset(document, lineNumber);
        String lineText = document.getText(lineOffset, caretOffset - lineOffset);

        if (isRemoveEmptyList()) {
            // remove empty list
            // e.g. If user presses enter key when caret position is after "- " (i.e. "- [here]"), "- " is removed.
            Matcher matcher = AFTER_INSERT_PATTERN.matcher(lineText);
            if (matcher.find()) {
                String hlText = matcher.group(LIST_GROUP);
                if (hlText != null) {
                    int length = hlText.length();
                    document.remove(caretOffset - length, length);
                }
            }
        }

        // reorder ordered list
        OrderedListReorderer reorderer = new OrderedListReorderer(context.getComponent(), document, caretOffset);
        reorderer.reorder(true);
    }

    @Override
    public void cancelled(Context context) {
    }

    private boolean isEnabled() {
        return MarkdownGlobalOptions.getInstance().isTypingHooks();
    }

    private boolean isRemoveEmptyList() {
        return MarkdownGlobalOptions.getInstance().isRemoveEmptyList();
    }

    private boolean isAutoAdditionList() {
        return MarkdownGlobalOptions.getInstance().isAutoAdditionList();
    }

    @MimeRegistration(mimeType = MarkdownLanguageConfig.MIME_TYPE, service = TypedBreakInterceptor.Factory.class)
    public static class MarkdownFactory implements TypedBreakInterceptor.Factory {

        @Override
        public TypedBreakInterceptor createTypedBreakInterceptor(MimePath mimePath) {
            return new MarkdownTypedBreakInterceptor();
        }
    }

}
