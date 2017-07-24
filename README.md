# guahao
改造自GsonFormat

GsonFormat源码地址:https://github.com/zzz40500/GsonFormat

采用intellij idea开发

intellij idea开发文档: http://www.jetbrains.org/intellij/sdk/docs/welcome.html

插件文档说明

使用

1.保存插件jar包到本地

2.打开AS---------->File------>Setting------->Plugins-------->Install plugin from disk----->选择保存的Jar包路径------>apply/OK----->重启AS

3.新建一个类文件，将光标置于文件（即当前焦点为该文件）----->选择Edit下的Guahao（快捷键 Ctrl+Alt+H）---->选择左下角的setting------>在Field选项栏下，选择field public------->OK（这个设置只需操作一次）

4.接下来跟GsonFormat的用法一样，将后台给的json字符串复制进去，format/OK,在弹窗中调整，点击OK，自动在之前光标之前所在的文件里生成代码。

源码解析

插件响应的入口方法是actionPerformed,即在你点下AS状态栏的选项后，开始执行这个方法

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        Editor editor = event.getData(PlatformDataKeys.EDITOR);
        PsiFile mFile = PsiUtilBase.getPsiFileInEditor(editor, project);
        //代表打开的文件，PsiJavaFile子类表示打开的是Java文件，PsiXmlFile代表打开的是xml文件，它们都是PsiFile的子类，即光标所在的文件，也是目标要自动生成代码的文件
        PsiClass psiClass = getTargetClass(editor, mFile);
        JsonDialog jsonD = new JsonDialog(psiClass, mFile, project);
        //JsonDialog,即接收json串的弹窗，我们点击AS状态栏后弹出的第一个弹窗
        jsonD.setClass(psiClass);
        jsonD.setFile(mFile);
        jsonD.setProject(project);
        jsonD.setSize(600, 400);
        jsonD.setLocationRelativeTo(null);
        jsonD.setVisible(true);
    
    }

JsonDialog的构造方法中，声明的响应监听操作，即initListener()。不同于Android绘制页面，弹窗使用Java的swing包组件绘制的，即JButton,JPanel等。

    public JsonDialog(PsiClass cls, PsiFile file, Project project) throws HeadlessException {
        this.cls = cls;
        this.file = file;
        this.project = project;
        setContentPane(contentPane2);
        setTitle("GuaHaoFormat");
        getRootPane().setDefaultButton(okButton);
        this.setAlwaysOnTop(true);
        initGeneratePanel(file);
        initListener();
    }

initListener()方法对弹窗的每个按钮做了监听，这里以OK为例，点击OK后，执行onOk方法

    private void initListener() {
        //OK,弹出编辑框，修改数据类型等
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (generateClassTF.isFocusOwner()) {
                    editTP.requestFocus(true);
                } else {
                    onOK();//执行核心操作
                }
            }
        });
        //格式化json字符串
        formatBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String json = editTP.getText();
                json = json.trim();
                if (json.startsWith("{")) {
                    JSONObject jsonObject = new JSONObject(json);
                    String formatJson = jsonObject.toString(4);
                    editTP.setText(formatJson);
                } else if (json.startsWith("[")) {
                    JSONArray jsonArray = new JSONArray(json);
                    String formatJson = jsonArray.toString(4);
                    editTP.setText(formatJson);
                }
    
            }
        });
        //编辑框入口
        editTP.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                super.keyReleased(keyEvent);
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                    onOK();
                }
            }
        });
        generateClassP.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                super.keyReleased(keyEvent);
                if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER) {
                    editTP.requestFocus(true);
                }
            }
        });
        errorLB.addMouseListener(new MouseAdapter() {
    
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                super.mouseClicked(mouseEvent);
                if (errorInfo != null) {
                    ErrorDialog errorDialog = new ErrorDialog(errorInfo);
                    errorDialog.setSize(800, 600);
                    errorDialog.setLocationRelativeTo(null);
                    errorDialog.setVisible(true);
                }
            }
        });
        //取消
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
        //设置
        settingButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                openSettingDialog();
            }
        });
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
        contentPane2.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

onOk方法（）,主要是获取json字符串，并做相应处理后，保存数据，传往编辑调整弹窗页面，即

 new ConvertBridge(this, jsonSTR, file, project, generateClass,cls, generateClassName).run();这行

    private void onOK() {
    
        this.setAlwaysOnTop(false);
        String jsonSTR = editTP.getText().trim();//获取数据字符串
        if (TextUtils.isEmpty(jsonSTR)) { //空，不做任何处理
            return;
        }
        //获取光标所在类文件名称
        String generateClassName = generateClassTF.getText().replaceAll(" ", "").replaceAll(".java$", "");
        if (TextUtils.isEmpty(generateClassName) || generateClassName.endsWith(".")) {
            Toast.make(project, generateClassP, MessageType.ERROR, "the path is not allowed");
            return;
        }
        PsiClass generateClass = null;
        if (!currentClass.equals(generateClassName)) {//获取的类名与当前光标所在类文件不匹配
            generateClass = PsiClassUtil.exist(file, generateClassTF.getText());
        } else {
            generateClass = cls; 
        }
    
        new ConvertBridge(this, jsonSTR, file, project, generateClass,
                cls, generateClassName).run();//操作符，内容串，所在文件，所在项目，目标类，当前类，类名
    }

ConverBridge.run方法里做了真正解析Json字符串的操作,这里解析Json串后，保存为相应的数据结构，核心为FieldEntity和ClassEntity这两个类（数据结构）

    public void run() {
        JSONObject json = null;
        operator.cleanErrorInfo();
        try {
    
            json = parseJSONObject(jsonStr);
        } catch (Exception e) {
            String jsonTS = removeComment(jsonStr);
            jsonTS = jsonTS.replaceAll("^.*?\\{", "{");
            try {
                json = parseJSONObject(jsonTS);
            } catch (Exception e2) {
                handleDataError(e2);
            }
        }
        if (json != null) {
            try {
                ClassEntity classEntity = collectClassAttribute(targetClass, Config.getInstant().isReuseEntity());
                if (classEntity != null) {
                    for (FieldEntity item : classEntity.getFields()) {
                        declareFields.put(item.getKey(), item);
                        CheckUtil.getInstant().addDeclareFieldName(item.getKey());
                    }
                }
                if (Config.getInstant().isSplitGenerate()) {
                    collectPackAllClassName();
                }
                operator.setVisible(false);
                parseJson(json);//核心操作
            } catch (Exception e2) {
                handleDataError(e2);
                operator.setVisible(true);
            }
        }
        declareFields = null;
        declareClass = null;
    }

最后在parseJson方法里做判断是否为处女模式，如果不是执行非处女模式（区别在与是否进入调整页面，setting里可以设置，默认勾选处女模式）

    private void parseJson(JSONObject json) {
        List<String> generateFiled = collectGenerateFiled(json);
        if (Config.getInstant().isVirgoMode()) {
            handleVirgoMode(json, generateFiled);//处女模式
        } else {
            handleNormal(json, generateFiled);//非处女模式
        }
    }

handleNormal里直接进行了生成代码的操作，即写入文件操作，由一个核心类DataWriter的run方法执行

    private void handleNormal(JSONObject json, List<String> generateFiled) {
        WriteCommandAction.runWriteCommandAction(project, new Runnable() {
            @Override
            public void run() {
                if (targetClass == null) {
                    try {
                        targetClass = PsiClassUtil.getPsiClass(file, project, generateClassName);
                    } catch (Throwable throwable) {
                        handlePathError(throwable);
                    }
                }
                if (targetClass != null) {
                    generateClassEntity.setPsiClass(targetClass);
                    try {
                        generateClassEntity.addAllFields(createFields(json, generateFiled, generateClassEntity));
                        operator.setVisible(false);
                        DataWriter dataWriter = new DataWriter(file, project, targetClass);
                        dataWriter.execute(generateClassEntity);
                        Config.getInstant().saveCurrentPackPath(packageName);
                        operator.dispose();
                    } catch (Exception e) {
                        throw e;
                    }
                }
            }
        });
    }

如果是处女模式，则将保存的数据结构和相关信息传到调整弹窗页面

    private void handleVirgoMode(JSONObject json, List<String> fieldList) {
        generateClassEntity.setClassName("");
        generateClassEntity.setPsiClass(targetClass);
        generateClassEntity.addAllFields(createFields(json, fieldList, generateClassEntity));
        FieldsDialog fieldsDialog = new FieldsDialog(operator, generateClassEntity, factory,
                targetClass, currentClass, file, project, generateClassName);//生成弹窗，数据通过构造方法传入
                //下面设置弹窗大小位置等外观
        fieldsDialog.setSize(800, 500);
        fieldsDialog.setLocationRelativeTo(null);
        fieldsDialog.setVisible(true);
    }

同理，FieldDialog里也设置了OK监听器，点击OK后，也执行自动生成代码的操作，即写入文件数据操作

    public FieldsDialog(ConvertBridge.Operator operator, ClassEntity classEntity,
                        PsiElementFactory factory, PsiClass psiClass, PsiClass aClass, PsiFile file, Project project
            , String generateClassStr) {
        this.operator = operator;
        this.factory = factory;
        this.aClass = aClass;
        this.file = file;
        this.project = project;
        this.psiClass = psiClass;
        this.generateClassStr = generateClassStr;
        setContentPane(contentPane);
        setTitle("Virgo Model");
        getRootPane().setDefaultButton(buttonOK);
        this.setAlwaysOnTop(true);
        initListener(classEntity, generateClassStr);//声明监听器
    }

    private void initListener(ClassEntity classEntity, String generateClassStr) {
            this.classEntity = classEntity;
            defaultMutableTreeTableNodeList = new ArrayList<DefaultMutableTreeTableNode>();
            JXTreeTable treetable = new JXTreeTable(new FiledTreeTableModel(createData(classEntity)));
            CheckTreeTableManager manager = new CheckTreeTableManager(treetable);
            manager.getSelectionModel().addPathsByNodes(defaultMutableTreeTableNodeList);
            treetable.getColumnModel().getColumn(0).setPreferredWidth(150);
    //        treetable.setSelectionBackground(treetable.getBackground());
            treetable.expandAll();
            treetable.setCellSelectionEnabled(false);
            final DefaultListSelectionModel defaultListSelectionModel = new DefaultListSelectionModel();
            treetable.setSelectionModel(defaultListSelectionModel);
    
            defaultListSelectionModel.setSelectionMode(SINGLE_SELECTION);
            defaultListSelectionModel.addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    defaultListSelectionModel.clearSelection();
                }
            });
            defaultMutableTreeTableNodeList = null;
            treetable.setRowHeight(30);
            sp.setViewportView(treetable);
            generateClass.setText(generateClassStr);
            buttonOK.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    onOK();//点击OK，开始执行生成代码操作
                }
            });
            buttonCancel.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    onCancel();
                }
            });
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    onCancel();
                }
            });
            contentPane.registerKeyboardAction(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    onCancel();
                }
            }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    
            contentPane.registerKeyboardAction(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    onOK();
                }
            }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        }

同非处女座模式一样，最后也是通过DataWriter这个核心类进行写入操作

    private void onOK() {
        this.setAlwaysOnTop(false);
        WriteCommandAction.runWriteCommandAction(project, new Runnable() {
    
            @Override
            public void run() {
                if (psiClass == null) {
                    try {
                        psiClass = PsiClassUtil.getPsiClass(file, project, generateClassStr);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                        operator.showError(ConvertBridge.Error.DATA_ERROR);
                        Writer writer = new StringWriter();
                        PrintWriter printWriter = new PrintWriter(writer);
                        throwable.printStackTrace(printWriter);
                        printWriter.close();
                        operator.setErrorInfo(writer.toString());
                        operator.setVisible(true);
                        operator.showError(ConvertBridge.Error.PATH_ERROR);
                    }
                }
    
                if (psiClass != null) {
                    String[] arg = generateClassStr.split("\\.");
                    if (arg.length > 1) {
                        Config.getInstant().setEntityPackName(generateClassStr.substring(0, generateClassStr.length() - arg[arg.length - 1].length()));
                        Config.getInstant().save();
                    }
                    try {
                        setVisible(false);
                        DataWriter dataWriter = new DataWriter(file, project, psiClass);
                        dataWriter.execute(classEntity);//执行生成代码操作
                        Config.getInstant().saveCurrentPackPath(StringUtils.getPackage(generateClassStr));
                        operator.dispose();
                        dispose();
                    } catch (Exception e) {
                        e.printStackTrace();
                        operator.showError(ConvertBridge.Error.PARSE_ERROR);
                        Writer writer = new StringWriter();
                        PrintWriter printWriter = new PrintWriter(writer);
                        e.printStackTrace(printWriter);
                        printWriter.close();
                        operator.setErrorInfo(writer.toString());
                        operator.setVisible(true);
                        dispose();
                    }
                }
            }
        });
    }

DataWriter里execute方法最终调用了run，其中 new ClassProcessor.generate的方法又调用了Processor的process方法，这里就是改造的核心地方，生成代码的各个地方，从字段，到方法。

    protected void run() {
        if (targetClass == null) {
            return;
        }
        generateClassList.clear();
        new ClassProcessor(factory, cls).generate(targetClass, new IProcessor() {//核心
            @Override
            public void onStarProcess(ClassEntity classEntity, PsiElementFactory factory, PsiClass cls) {
                generateClassList.add(cls.getQualifiedName());
            }
    
            @Override
            public void onEndProcess(ClassEntity classEntity, PsiElementFactory factory, PsiClass cls) {
    
            }
    
            @Override
            public void onStartGenerateClass(PsiElementFactory factory, ClassEntity classEntity, PsiClass parentClass) {
    
            }
    
            @Override
            public void onEndGenerateClass(PsiElementFactory factory, ClassEntity classEntity, PsiClass parentClass, PsiClass generateClass) {
                generateClassList.add(generateClass.getQualifiedName());
    
            }
        });
    }

process方法，其中注释掉的是GsomFormat原有的set/get方法，加上了我们特有的代码生成代码generateConstructMethod

        public void process(ClassEntity classEntity, PsiElementFactory factory, PsiClass cls, IProcessor visitor) {
            mainPackage = PsiClassUtil.getPackage(cls);
            onStarProcess(classEntity, factory, cls, visitor);
    
            for (FieldEntity fieldEntity : classEntity.getFields()) {
                generateField(factory, fieldEntity, cls, classEntity);//处理字段
            }
            for (ClassEntity innerClass : classEntity.getInnerClasss()) {
                generateClass(factory, innerClass, cls, visitor);//处理引用对象的字段
            }
    
    //        generateGetterAndSetter(factory, cls, classEntity);
            generateConvertMethod(factory, cls, classEntity);
            generateConstructMethod(factory,cls,classEntity);
            onEndProcess(classEntity, factory, cls, visitor);
        }

