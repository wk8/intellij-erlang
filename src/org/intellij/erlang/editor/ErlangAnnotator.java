package org.intellij.erlang.editor;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.intellij.erlang.psi.*;
import org.jetbrains.annotations.NotNull;

import static org.intellij.erlang.psi.impl.ErlangPsiImplUtil.*;

/**
 * @author ignatov
 */
public class ErlangAnnotator implements Annotator, DumbAware {
  @Override
  public void annotate(@NotNull PsiElement psiElement, @NotNull final AnnotationHolder annotationHolder) {
    psiElement.accept(new ErlangVisitor() {
      @Override
      public void visitQVar(@NotNull ErlangQVar o) {
        setHighlighting(o, annotationHolder, ErlangSyntaxHighlighter.VARIABLES);
        if (inDefinition(o) || isLeftPartOfAssignment(o) || inAtomAttribute(o) || isMacros(o)  || inSpecification(o)) return;
        markIfUnresolved(o, o, annotationHolder, "Unresolved variable " + o.getText());
      }

      @Override
      public void visitRecordExpression(@NotNull ErlangRecordExpression o) {
        ErlangQAtom atomName = o.getAtomName();
        if (atomName != null) {
          PsiElement prevSibling = atomName.getPrevSibling();
          if (prevSibling != null && "#".equals(prevSibling.getText())) {
            markIfUnresolved(o, atomName, annotationHolder, "Unresolved record " + atomName.getText());
          }
        }
      }

      @Override
      public void visitAtomAttribute(@NotNull ErlangAtomAttribute o) {
        setHighlighting(o.getQAtom(), annotationHolder, ErlangSyntaxHighlighter.KEYWORD);
      }

      @Override
      public void visitCallbackSpec(@NotNull ErlangCallbackSpec o) {
        markFirstChildAsKeyword(o, annotationHolder);
      }

      @Override
      public void visitSpecification(@NotNull ErlangSpecification o) {
        markFirstChildAsKeyword(o, annotationHolder);
      }

      @Override
      public void visitAttribute(@NotNull ErlangAttribute o) {
        markFirstChildAsKeyword(o, annotationHolder);
      }

      @Override
      public void visitExport(@NotNull ErlangExport o) {
        markFirstChildAsKeyword(o, annotationHolder);
      }

      @Override
      public void visitInclude(@NotNull ErlangInclude o) {
        markFirstChildAsKeyword(o, annotationHolder);
        markAttributeNameAsKeyword(o, annotationHolder, "include");
      }

      @Override
      public void visitModule(@NotNull ErlangModule o) {
        markFirstChildAsKeyword(o, annotationHolder);
        String ext = FileUtil.getExtension(o.getContainingFile().getName());
        String withoutExtension = FileUtil.getNameWithoutExtension(o.getContainingFile().getName());
        String moduleName = o.getName();
        ErlangCompositeElement atom = o.getQAtom();
        if (atom != null && !moduleName.equals(withoutExtension)) {
          annotationHolder.createErrorAnnotation(atom, "Module with name '" + moduleName + "' should be declared in a file named '" +
            moduleName + "." + ext + "'.");
        }
      }

      @Override
      public void visitRecordDefinition(@NotNull ErlangRecordDefinition o) {
        markFirstChildAsKeyword(o, annotationHolder);
        markAttributeNameAsKeyword(o, annotationHolder, "record");
      }

      @Override
      public void visitExportFunction(@NotNull ErlangExportFunction o) {
        markIfUnresolved(o, o, annotationHolder, "Unresolved function " + o.getText());
      }

      // todo: add export, import and other bundled attributes
    });
  }

  private static void markAttributeNameAsKeyword(ErlangCompositeElement o, AnnotationHolder annotationHolder, String name) {
    PsiElement rec = o.getFirstChild();
    while (rec != null) {
      if (rec instanceof LeafPsiElement && name.equals(rec.getText())) break;
      rec = rec.getNextSibling();
    }
    if (rec != null) {
      setHighlighting(rec, annotationHolder, ErlangSyntaxHighlighter.KEYWORD);
    }
  }

  private static void markIfUnresolved(ErlangCompositeElement o, ErlangCompositeElement errorElement, AnnotationHolder annotationHolder, String text) {
    PsiReference reference = o.getReference();
    if (reference != null && reference.resolve() == null) {
      annotationHolder.createErrorAnnotation(errorElement, text);
    }
  }

  private static void markFirstChildAsKeyword(ErlangCompositeElement o, AnnotationHolder annotationHolder) {
    final PsiElement firstChild = o.getFirstChild();
    if (firstChild != null) {
      setHighlighting(firstChild, annotationHolder, ErlangSyntaxHighlighter.KEYWORD);
    }
  }

  private static void setHighlighting(@NotNull PsiElement element, @NotNull AnnotationHolder holder, final TextAttributesKey key) {
    holder.createInfoAnnotation(element, null).setEnforcedTextAttributes(TextAttributes.ERASE_MARKER);
    holder.createInfoAnnotation(element, null).setEnforcedTextAttributes(EditorColorsManager.getInstance().getGlobalScheme().getAttributes(key));
  }
}