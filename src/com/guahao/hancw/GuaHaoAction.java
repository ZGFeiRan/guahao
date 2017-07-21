package com.guahao.hancw;

import com.guahao.hancw.ui.JsonDialog;
import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;

/**
 * Created by Administrator on 2017/7/3 0003.
 */
public class GuaHaoAction extends BaseGenerateAction {

    @SuppressWarnings("unused")
    public GuaHaoAction() {
        super(null);
    }

    @SuppressWarnings("unused")
    public GuaHaoAction(CodeInsightActionHandler handler) {
        super(handler);
    }

    @Override
    protected boolean isValidForClass(final PsiClass targetClass) {
        return super.isValidForClass(targetClass);
    }

    @Override
    public boolean isValidForFile(Project project, Editor editor, PsiFile file) {
        return super.isValidForFile(project, editor, file);
    }
    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        Editor editor = event.getData(PlatformDataKeys.EDITOR);
        PsiFile mFile = PsiUtilBase.getPsiFileInEditor(editor, project);//代表打开的文件，PsiJavaFile子类
        //表示打开的是Java文件，PsiXmlFile代表打开的是xml文件，它们都是PsiFile的子类
        PsiClass psiClass = getTargetClass(editor, mFile);
        JsonDialog jsonD = new JsonDialog(psiClass, mFile, project);//从窗口获取字符串
        jsonD.setClass(psiClass);
        jsonD.setFile(mFile);
        jsonD.setProject(project);
        jsonD.setSize(600, 400);
        jsonD.setLocationRelativeTo(null);
        jsonD.setVisible(true);

    }
}
