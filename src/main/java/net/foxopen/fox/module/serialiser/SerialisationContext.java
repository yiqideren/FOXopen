package net.foxopen.fox.module.serialiser;

import net.foxopen.fox.App;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.auth.AuthenticatedUser;
import net.foxopen.fox.banghandler.InternalAuthLevel;
import net.foxopen.fox.command.XDoResult;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.download.DownloadLinkXDoResult;
import net.foxopen.fox.entrypoint.uri.RequestURIBuilder;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.module.State;
import net.foxopen.fox.module.datadefinition.EvaluatedDataDefinition;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.fieldset.FieldSet;
import net.foxopen.fox.module.parsetree.EvaluatedModalPopover;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedBufferPresentationNode;
import net.foxopen.fox.module.parsetree.evaluatedpresentationnode.EvaluatedPresentationNode;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import net.foxopen.fox.thread.FocusResult;
import net.foxopen.fox.thread.PopupXDoResult;
import net.foxopen.fox.thread.alert.AlertMessage;
import net.foxopen.fox.thread.devtoolbar.DevToolbarContext;
import net.foxopen.fox.thread.stack.transform.ModelessCall;
import net.foxopen.fox.thread.storage.TempResource;
import net.foxopen.fox.thread.storage.TempResourceGenerator;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SerialisationContext {

  App getApp();

  Mod getModule();

  EvaluatedPresentationNode<? extends PresentationNode> evaluateNode(EvaluatedPresentationNode<? extends PresentationNode> pParent, PresentationNode pNode, DOM pEvalContext);

  State getState();

  ContextUElem getContextUElem();

  FieldSet getFieldSet();

  FocusResult getFocusResult();

  List<AlertMessage> getAlertMessages();

  List<DownloadLinkXDoResult> getDownloadLinks();

  List<PopupXDoResult> getPopups();

  List<ModelessCall.ModelessPopup> getModelessPopups();

  <T extends XDoResult> List<T> getXDoResultList(Class<T> pForClass);

  EvaluatedModalPopover getCurrentModalPopoverOrNull();

  boolean isAccessibilityMode();

  String getRequestLogId();

  ThreadInfoProvider getThreadInfoProvider();

  List<EvaluatedBufferPresentationNode> getBufferRegions();

  Set<WidgetBuilderType> getImplicatedWidgets();

  Collection<? extends EvaluatedNode> getEvaluatedNodesByWidgetBuilderType(WidgetBuilderType pWidgetBuilderType);

  void addConditionalLoadJavascript(String pJS);

  void addUnconditionalLoadJavascript(String pJS);

  List<String> getUnconditionalLoadJavascript();

  List<String> getConditionalLoadJavascript();

  DevToolbarContext getDevToolbarContext();

  TempResource<?> createTempResource(TempResourceGenerator pGenerator);

  /**
   * @see net.foxopen.fox.entrypoint.uri.RequestURIBuilder#buildStaticResourceURI
   * @param pResourcePath
   * @return
   */
  String getStaticResourceURI(String pResourcePath);

  /**
   * @see net.foxopen.fox.entrypoint.uri.RequestURIBuilder#buildContextResourceURI
   * @param pResourcePath
   * @return
   */
  String getContextResourceURI(String pResourcePath);

  /**
   * @see net.foxopen.fox.entrypoint.uri.RequestURIBuilder#buildStaticResourceOrFixedURI
   * @param pResourcePathOrFixedURI
   * @return
   */
  String getStaticResourceOrFixedURI(String pResourcePathOrFixedURI);

  /**
   * @see net.foxopen.fox.entrypoint.uri.RequestURIBuilder#buildTempResourceURI
   * @param pTempResource
   * @param pReadableName
   * @return
   */
  String getTempResourceURI(TempResource<?> pTempResource, String pReadableName);

  /**
   * @see net.foxopen.fox.entrypoint.uri.RequestURIBuilder#buildImageURI
   * @param pImageURL
   * @return
   */
  String getImageURI(String pImageURL);

  /**
   * Creates a new URIBuilder for constructing parameterised requests. For simple request building, use one of the
   * getXXXURI methods.
   * @return New URI builder.
   */
  RequestURIBuilder createURIBuilder();

  InternalAuthLevel getInternalAuthLevel();

  Optional<AuthenticatedUser> getAuthenticatedUser();

  List<EvaluatedDataDefinition> getEvaluatedDataDefinitions();

  /**
   * @return Gets the number of client visibility rules which have been evaluated by this SerialisationContext.
   */
  int getClientVisibilityRuleCount();

  /**
   * Resolves a buffer by name and evaluates it. If the buffer name is invalid, an exception is thrown.
   * @param pBufferName Name of buffer to evaluate.
   * @param pBufferAttach Attach point for buffer evaluation.
   * @return The evaluated buffer.
   */
  EvaluatedBufferPresentationNode evaluateBuffer(String pBufferName, DOM pBufferAttach);
}
