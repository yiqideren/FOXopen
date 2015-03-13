package net.foxopen.fox.module.parsetree.evaluatedpresentationnode;

import java.util.ArrayList;
import java.util.List;

import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.module.parsetree.EvaluatedParseTree;
import net.foxopen.fox.module.parsetree.presentationnode.PresentationNode;
import net.foxopen.fox.module.serialiser.OutputSerialiser;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.ComponentBuilder;
import net.foxopen.fox.module.serialiser.components.ComponentBuilderType;


/**
 * Abstract class for all Evaluated Presentation Nodes. Evaluated Presentation Nodes are constructed to run XPaths and
 * cache the results and other objects to be looked over at serialisation time.
 */
public abstract class EvaluatedPresentationNode<PN extends PresentationNode> {
  // Child nodes
  private final List<EvaluatedPresentationNode<? extends PresentationNode>> mChildren = new ArrayList<>();

  private final PN mOriginalNode;

  private final EvaluatedPresentationNode mParentNode;

  // Context to use when evaluating XPaths
  private DOM mEvalContext = null;

  /**
   * Construct a EvaluatedPresentationNode to go into an Evaluated Parse Tree
   *
   * @param pParentNode Parent node in the Evaluated Parse Tree
   * @param pOriginalNode Non-evaluated PresentationNode from the Parse Tree
   * @param pEvalParseTree Evaluated Parse Tree to evaluate with
   * @param pEvalContext DOM context to evaluate against
   */
  public EvaluatedPresentationNode(EvaluatedPresentationNode<? extends PresentationNode> pParentNode, PN pOriginalNode, DOM pEvalContext) {
    mParentNode = pParentNode;
    mOriginalNode = pOriginalNode;
    mEvalContext = pEvalContext;
  }

  public EvaluatedPresentationNode getParentNode() {
    return mParentNode;
  }

  /**
   * Evaluated the children of an EvaulatedPresentationNode
   *
   * @param pEvaluatedParseTree
   */
  public void evaluateChildren(EvaluatedParseTree pEvaluatedParseTree) {
    List<EvaluatedPresentationNode<? extends PresentationNode>> lEvaluatedChildren = new ArrayList<>();

    for (PresentationNode lChild : mOriginalNode.getChildNodes()) {
      EvaluatedPresentationNode<? extends PresentationNode> lEvalChild = pEvaluatedParseTree.evaluateNode(this, lChild, mEvalContext);
      if (lEvalChild != null) {
        lEvaluatedChildren.add(lEvalChild);
      }
    }

    addChildren(lEvaluatedChildren);
  }

  public void addChild(EvaluatedPresentationNode<? extends PresentationNode> pNode) {
    mChildren.add(pNode);
  }

  public void addChildren(List<EvaluatedPresentationNode<? extends PresentationNode>> pChildren) {
    mChildren.addAll(pChildren);
  }

  public List<EvaluatedPresentationNode<? extends PresentationNode>> getChildren() {
    return mChildren;
  }

  /**
   * Overloadable function to shortcut simple text returning nodes during serialisation
   * @return throws an UnsupportedOperationException()
   */
  public String getText() {
    throw new UnsupportedOperationException("This evaluated presentation node does not hold a single text value to get");
  }

  public boolean isEscapingRequired() {
    throw new UnsupportedOperationException("This evaluated presentation node does not hold a single text value that may or may not need escaping");
  }

  /**
   * Overloadable function to identify page component node
   * @return throws an UnsupportedOperationException()
   */
  public abstract ComponentBuilderType getPageComponentType();

  /**
   * Render the current presentation node using an output serialiser
   *
   * @param pSerialiser Output serialiser to render with
   */
  public void render(SerialisationContext pSerialisationContext, OutputSerialiser pSerialiser) {
    //Force a raw type - should be safe but the compiler complains
    ComponentBuilder lBuilder = pSerialiser.getComponentBuilder(getPageComponentType());
    lBuilder.buildComponent(pSerialisationContext, pSerialiser, this);
  }

  public boolean canEvaluateChildren() {
    return true;
  }

  public abstract String toString();

  public void setEvalContext(DOM pEvalContext) {
    mEvalContext = pEvalContext;
  }

  public DOM getEvalContext() {
    return mEvalContext;
  }

  public PN getOriginalNode() {
    return mOriginalNode;
  }

  /**
   * Searches this EPN's parent hierarchy for an EPN of the given type. The first ancestor encountered is returned.
   * If this EPN has no such ancestors, null is returned.
   * @param <T>
   * @param pAncestorType EPN type to search for.
   * @return Ancestor EPN or null.
   */
  public <T extends EvaluatedPresentationNode> T getClosestAncestor(Class<T> pAncestorType) {
    EvaluatedPresentationNode lAncestor = this;
    do {
      lAncestor = lAncestor.getParentNode();

      if(pAncestorType.isInstance(lAncestor)) {
        return pAncestorType.cast(lAncestor);
      }
    }
    while (lAncestor != null);

    return null;
  }
}