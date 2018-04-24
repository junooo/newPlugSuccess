package com.intellij.codeInspection;

import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.codeInspection.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.StringTokenizer;

/**
 * @author max
 */
public class ComparingReferencesInspection extends BaseJavaLocalInspectionTool {
  private static final Logger LOG = Logger.getInstance("#com.com.intellij.codeInspection.com.intellij.codeInspection.ComparingReferencesInspection");

  private final LocalQuickFix myQuickFix = new MyQuickFix();

  @SuppressWarnings({"WeakerAccess"})
  @NonNls
  public String CHECKED_CLASSES = "java.lang.String;java.util.Date";
  @NonNls
  private static final String DESCRIPTION_TEMPLATE =
      InspectionsBundle.message("inspection.comparing.references.problem.descriptor");

  @NotNull
  public String getDisplayName() {

    return "'==' or '!=' instead of 'equals()'";
  }

  @NotNull
  public String getGroupDisplayName() {
    return GroupNames.BUGS_GROUP_NAME;
  }

  //对应html

  @NotNull
  public String getShortName() {
    return "ComparingReferences";
  }

  private boolean isCheckedType(PsiType type) {
    if (!(type instanceof PsiClassType)) return false;

    StringTokenizer tokenizer = new StringTokenizer(CHECKED_CLASSES, ";");
    while (tokenizer.hasMoreTokens()) {
      String className = tokenizer.nextToken();
      if (type.equalsToText(className)) return true;
    }

    return false;
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
    return new JavaElementVisitor() {

      @Override
      public void visitReferenceExpression(PsiReferenceExpression psiReferenceExpression) {

      }



      /**
       *
       * @param expression
       */
      @Override
      public void visitBinaryExpression(PsiBinaryExpression expression) {
        //执行？？
        super.visitBinaryExpression(expression);

        //获取特殊符号？
        IElementType opSign = expression.getOperationTokenType();

        //判断符号是否是EQEQ,NE
        if (opSign == JavaTokenType.EQEQ || opSign == JavaTokenType.NE) {

          //获取左侧字符，右侧字符
          PsiExpression lOperand = expression.getLOperand();
          PsiExpression rOperand = expression.getROperand();

          //右 不能为空；左右不能为空的字符串
          if (rOperand == null || isNullLiteral(lOperand) || isNullLiteral(rOperand)) return;

          //比较左右的类型
          PsiType lType = lOperand.getType();
          PsiType rType = rOperand.getType();

          //如果左右都不是字符
          if (isCheckedType(lType) || isCheckedType(rType)) {

            //向用户报错，提示 expression什么含义？ +问题描述 +快速修复
            //队列塞
            holder.registerProblem(expression,
                                   DESCRIPTION_TEMPLATE, myQuickFix);
          }
        }
      }
    };
  }

  private static boolean isNullLiteral(PsiExpression expr) {
    return expr instanceof PsiLiteralExpression && "null".equals(expr.getText());
  }

  private static class MyQuickFix implements LocalQuickFix {
    @NotNull
    public String getName() {
      // The test (see the TestThisPlugin class) uses this string to identify the quick fix action.
      return InspectionsBundle.message("inspection.comparing.references.use.quickfix");
    }


    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      try {
        PsiBinaryExpression binaryExpression = (PsiBinaryExpression) descriptor.getPsiElement();
        IElementType opSign = binaryExpression.getOperationTokenType();
        PsiExpression lExpr = binaryExpression.getLOperand();
        PsiExpression rExpr = binaryExpression.getROperand();
        if (rExpr == null)
          return;

        //equal 去做替换
        PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
        PsiMethodCallExpression equalsCall =
            (PsiMethodCallExpression) factory.createExpressionFromText("a.equals(b)", null);

        equalsCall.getMethodExpression().getQualifierExpression().replace(lExpr);
        equalsCall.getArgumentList().getExpressions()[0].replace(rExpr);

        PsiExpression result = (PsiExpression) binaryExpression.replace(equalsCall);

        //replace PSI
        if (opSign == JavaTokenType.NE) {
          PsiPrefixExpression negation = (PsiPrefixExpression) factory.createExpressionFromText("!a", null);
          negation.getOperand().replace(result);
          result.replace(negation);
        }
      } catch (IncorrectOperationException e) {
        LOG.error(e);
      }
    }

    @NotNull
    public String getFamilyName() {
      return getName();
    }
  }

  public JComponent createOptionsPanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    final JTextField checkedClasses = new JTextField(CHECKED_CLASSES);
    checkedClasses.getDocument().addDocumentListener(new DocumentAdapter() {
      public void textChanged(DocumentEvent event) {
        CHECKED_CLASSES = checkedClasses.getText();
      }
    });

    panel.add(checkedClasses);
    return panel;
  }

  public boolean isEnabledByDefault() {
    return true;
  }
}
