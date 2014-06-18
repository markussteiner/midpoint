/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.admin.reports;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.PrismPropertyModel;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.page.admin.configuration.dto.InputStringValidator;
import com.evolveum.midpoint.web.page.admin.reports.component.AceEditorPanel;
import com.evolveum.midpoint.web.page.admin.reports.component.ReportConfigurationPanel;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.web.util.Base64Model;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.StringValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author shood
 */
@PageDescriptor(url = "/admin/report", encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = PageAdminReports.AUTH_REPORTS_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
                description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.NS_AUTHORIZATION + "#report",
                label = "PageReport.auth.report.label",
                description = "PageReport.auth.report.description")})
public class PageReport<T extends Serializable> extends PageAdminReports {

    private static Trace LOGGER = TraceManager.getTrace(PageReport.class);

    private static final String DOT_CLASS = PageReport.class.getName() + ".";
    private static final String OPERATION_LOAD_REPORT = DOT_CLASS + "loadReport";
    private static final String OPERATION_SAVE_REPORT = DOT_CLASS + "saveReport";
    private static final String OPERATION_RUN_REPORT = DOT_CLASS + "runReport";
    private static final String OPERATION_VALIDATE_REPORT = DOT_CLASS + "validateReport";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TAB_PANEL = "tabPanel";
    private static final String ID_SAVE_BUTTON = "save";
    private static final String ID_CANCEL_BUTTON = "cancel";

    private static final int FULL_XML_TAB_INDEX = 3;
    private int previousTabIndex = 0;
    private AceEditorPanel fullXmlEditorPanel;
    private String fullXmlEditorPanelString;

    private LoadableModel<PrismObject<ReportType>> model;

    public PageReport() {
        model = new LoadableModel<PrismObject<ReportType>>(false) {

            @Override
            protected PrismObject<ReportType> load() {
                return loadReport();
            }
        };

        initLayout();
    }

    private PrismObject<ReportType> loadReport() {
        StringValue reportOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);

        OperationResult result = new OperationResult(OPERATION_LOAD_REPORT);
        PrismObject<ReportType> prismReport = WebModelUtils.loadObject(ReportType.class, reportOid.toString(), result, this);

        if (prismReport == null) {
            LOGGER.error("Couldn't load report.");
            throw new RestartResponseException(PageReports.class);
        }

        return prismReport;
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        List<ITab> tabs = new ArrayList<>();
        tabs.add(new AbstractTab(createStringResource("PageReport.basic")) {

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return new ReportConfigurationPanel(panelId, model);
            }
        });
        tabs.add(new AbstractTab(createStringResource("PageReport.jasperTemplate")) {

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                IModel<String> title = PageReport.this.createStringResource("PageReport.jasperTemplate");
                IModel<String> data = new Base64Model(new PrismPropertyModel<>(model, ReportType.F_TEMPLATE));
                return new AceEditorPanel(panelId, title, data);
            }
        });
        tabs.add(new AbstractTab(createStringResource("PageReport.jasperTemplateStyle")) {

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                IModel<String> title = PageReport.this.createStringResource("PageReport.jasperTemplateStyle");
                IModel<String> data = new Base64Model(new PrismPropertyModel<>(model, ReportType.F_TEMPLATE_STYLE));
                return new AceEditorPanel(panelId, title, data);
            }
        });
        tabs.add(new AbstractTab(createStringResource("PageReport.fullXml")) {

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                IModel<String> title = PageReport.this.createStringResource("PageReport.fullXml");
                fullXmlEditorPanelString = getStringFromObject();
                fullXmlEditorPanel = new AceEditorPanel(panelId, title, createFullXmlModel());
                return fullXmlEditorPanel;
            }
        });

        TabbedPanel reportTabPanel = new TabbedPanel(ID_TAB_PANEL, tabs){

            @Override
            protected WebMarkupContainer newLink(final String linkId, final int index){

                return new SubmitLink(linkId){

                    @Override
                    public void onSubmit(){
                        if(previousTabIndex == FULL_XML_TAB_INDEX){
                            fullXmlEditorPanelString = fullXmlEditorPanel.getEditor().getInput();

                            if(isReportValid(fullXmlEditorPanel.getEditor().getInput(), true)){
                                setSelectedTab(index);
                                previousTabIndex = index;
                            } else {
                                setSelectedTab(previousTabIndex);
                            }
                        } else{
                            setSelectedTab(index);
                            previousTabIndex = index;
                        }
                    }
                };
            }
        };

        mainForm.add(reportTabPanel);

        initButtons(mainForm);
    }

    private boolean isReportValid(String object, boolean showError){
        OperationResult result = new OperationResult(OPERATION_VALIDATE_REPORT);

        try {
            Holder<PrismObject<ReportType>> reportHolder = new Holder<PrismObject<ReportType>>(null);
            validateObject(object, reportHolder, true, result);

            if(result.isAcceptable()){
                return true;
            }

        } catch (Exception ex){
            if(showError){
                result.recordFatalError("Could not save object.", ex);
                showResultInSession(result);
            }
        }

        return false;
    }

    private String getStringFromObject(){
        PrismObject report = model.getObject();
        if (report == null) {
            return null;
        }

        try {
            return getPrismContext().serializeObjectToString(report, PrismContext.LANG_XML);
        } catch (SchemaException ex) {
            error("Could not create XML object from report.");
            throw new RestartResponseException(PageError.class);
        }
    }

    private IModel<String> createFullXmlModel() {
        return new IModel<String>() {

            @Override
            public String getObject() {
                if(fullXmlEditorPanel.getEditor().getInput() != null){
                    if(fullXmlEditorPanel.getEditor().getInput().isEmpty()){
                        return fullXmlEditorPanelString;
                    } else if(isReportValid(fullXmlEditorPanel.getEditor().getInput(), false)){
                        return fullXmlEditorPanelString;
                    } else {
                        return fullXmlEditorPanel.getEditor().getInput();
                    }
                } else {
                    return fullXmlEditorPanelString;
                }
            }

            @Override
            public void setObject(String object) {
                if(StringUtils.isEmpty(object)){
                    error(getString("PageReport.message.cantSaveEmpty"));
                    return;
                }

                OperationResult result = new OperationResult(OPERATION_VALIDATE_REPORT);

                if(isReportValid(object, false)){
                    Holder<PrismObject<ReportType>> reportHolder = new Holder<PrismObject<ReportType>>(null);
                    validateObject(object, reportHolder, true, result);

                    model.setObject(reportHolder.getValue());
                }

                fullXmlEditorPanelString = object;
            }

            @Override
            public void detach() {
            }
        };
    }

    private void initButtons(Form mainForm) {
        AjaxSubmitButton save = new AjaxSubmitButton(ID_SAVE_BUTTON, createStringResource("PageBase.button.save")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                onSavePerformed(target);
            }
        };
        mainForm.add(save);

        AjaxSubmitButton cancel = new AjaxSubmitButton(ID_CANCEL_BUTTON, createStringResource("PageBase.button.cancel")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                onCancelPerformed(target);
            }
        };
        mainForm.add(cancel);
    }

    protected void onSavePerformed(AjaxRequestTarget target) {
        OperationResult result = new OperationResult(OPERATION_SAVE_REPORT);
        try {
            Task task = createSimpleTask(OPERATION_SAVE_REPORT);

            PrismObject<ReportType> newReport = model.getObject();
            PrismObject<ReportType> oldReport = WebModelUtils.loadObject(ReportType.class, newReport.getOid(),
                    result, this);

            if (oldReport != null) {
                ObjectDelta<ReportType> delta = oldReport.diff(newReport);
                getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);
            }
        } catch (Exception e) {
            result.recordFatalError("Couldn't save report.", e);
        } finally {
            result.computeStatusIfUnknown();
        }

        if (WebMiscUtil.isSuccessOrHandledError(result)) {
            showResultInSession(result);
            setResponsePage(PageReports.class);
        } else {
            showResult(result);
            target.add(getFeedbackPanel());
        }
    }

    protected void onCancelPerformed(AjaxRequestTarget target) {
        setResponsePage(PageReports.class);
    }
}
