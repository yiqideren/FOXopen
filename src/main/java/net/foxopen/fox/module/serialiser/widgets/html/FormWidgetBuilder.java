package net.foxopen.fox.module.serialiser.widgets.html;

import com.google.common.base.Joiner;
import net.foxopen.fox.module.LayoutDirection;
import net.foxopen.fox.module.datanode.EvaluatedNode;
import net.foxopen.fox.module.datanode.EvaluatedNodeInfoCollection;
import net.foxopen.fox.module.datanode.NodeAttribute;
import net.foxopen.fox.module.serialiser.FOXGridUtils;
import net.foxopen.fox.module.serialiser.SerialisationContext;
import net.foxopen.fox.module.serialiser.components.html.SingleWidgetBuildHelper;
import net.foxopen.fox.module.serialiser.html.HTMLSerialiser;
import net.foxopen.fox.module.serialiser.layout.GridLayoutManager;
import net.foxopen.fox.module.serialiser.layout.items.LayoutItem;
import net.foxopen.fox.module.serialiser.layout.items.LayoutItemEnum;
import net.foxopen.fox.module.serialiser.layout.items.LayoutWidgetItemColumn;
import net.foxopen.fox.module.serialiser.widgets.FormWidgetUtils;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilder;
import net.foxopen.fox.module.serialiser.widgets.WidgetBuilderType;
import net.foxopen.fox.thread.devtoolbar.DevToolbarContext;
import net.foxopen.fox.track.Track;

import java.util.ArrayList;
import java.util.List;


public class FormWidgetBuilder extends WidgetBuilderHTMLSerialiser<EvaluatedNodeInfoCollection> {
  private static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfoCollection>  INSTANCE = new FormWidgetBuilder();

  public static final WidgetBuilder<HTMLSerialiser, EvaluatedNodeInfoCollection> getInstance() {
    return INSTANCE;
  }

  private FormWidgetBuilder () {
  }

  @Override
  public boolean hasPrompt(EvaluatedNodeInfoCollection pEvalNode) {
    EvaluatedNode lFormParent = pEvalNode.getParent();
    if (lFormParent != null && lFormParent.getWidgetBuilderType() == WidgetBuilderType.FORM) {
      // Nested forms do have a prompt
      return pEvalNode.hasPrompt();
    }
    else {
      return false;
    }
  }

  @Override
  public void buildWidget(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNodeInfoCollection pEvalNode) {
    buildWidgetInternal(pSerialisationContext, pSerialiser, pEvalNode);
  }

  @Override
  public void buildWidgetInternal(SerialisationContext pSerialisationContext, HTMLSerialiser pSerialiser, EvaluatedNodeInfoCollection pEvalNode) {
    Track.pushDebug("FormWidget", pEvalNode.getDataItem().absolute());
    try {
      int lFormColumns = FormWidgetUtils.getFormColumns(pEvalNode);

      GridLayoutManager lFormLayout = new GridLayoutManager(lFormColumns, pSerialiser, pEvalNode);

      if (pEvalNode.hasChildren()) {
        pSerialiser.append("<div class=\"container setoutForm");

        List<String> lClasses = pEvalNode.getStringAttributes(NodeAttribute.CLASS, NodeAttribute.FORM_CLASS);
        List<String> lStyles = pEvalNode.getStringAttributes(NodeAttribute.STYLE, NodeAttribute.FORM_STYLE);

        // If pEvalNode is nested in another table/form it should add the nested class, if one is specified
        if (pEvalNode.isNested()) {
          lClasses.addAll(pEvalNode.getStringAttributes(NodeAttribute.NESTED_CLASS, NodeAttribute.NESTED_FORM_CLASS));
          lStyles.addAll(pEvalNode.getStringAttributes(NodeAttribute.NESTED_STYLE, NodeAttribute.NESTED_FORM_STYLE));
        }
        else {
          lClasses.addAll(pEvalNode.getStringAttributes(NodeAttribute.TOP_LEVEL_FORM_CLASS));
          lStyles.addAll(pEvalNode.getStringAttributes(NodeAttribute.TOP_LEVEL_FORM_STYLE));
        }

        if (lClasses.size() > 0) {
          pSerialiser.append(" ");
          pSerialiser.append(Joiner.on(" ").join(lClasses));
        }
        pSerialiser.append("\"");

        if (lStyles.size() > 0) {
          pSerialiser.append(" style=\"");
          pSerialiser.append(Joiner.on(" ").join(lStyles));
          pSerialiser.append("\"");
        }

        HTMLSerialiser.serialiseDataAttributes(pSerialiser, pEvalNode);

        pSerialiser.append(">");

        for (LayoutItem lItem : lFormLayout.getLayoutItems()) {
          if (lItem.getItemType() == LayoutItemEnum.ROW_START) {
            pSerialiser.append("<div class=\"row\">");
          }
          else if (lItem.getItemType() == LayoutItemEnum.ROW_END) {
            pSerialiser.append("</div>");
          }
          else if (lItem.getItemType() == LayoutItemEnum.COLUMN) {
            LayoutWidgetItemColumn lColumnItem = (LayoutWidgetItemColumn)lItem;
            pSerialiser.append("<div class=\"");
            pSerialiser.append(FOXGridUtils.calculateColumnClassName(lColumnItem.getColSpan(), lFormColumns));
            pSerialiser.append(" columns");

            List<String> lCellClasses = new ArrayList<>();
            List<String> lCellStyles = new ArrayList<>();

            if(!lColumnItem.isFiller()) {
              //For prompts and widgets (i.e. non-filler cells), add any extra classes which might be needed (i.e. client visibility initial state)
              lCellClasses.addAll(lColumnItem.getItemNode().getCellInternalClasses());
            }

            if (lColumnItem.isPrompt()) {
              // If it's a cell for a prompt
              lCellClasses.addAll(lColumnItem.getItemNode().getStringAttributes(NodeAttribute.PROMPT_CLASS, NodeAttribute.FORM_CELL_CLASS, NodeAttribute.CELL_CLASS));
              lCellStyles.addAll(lColumnItem.getItemNode().getStringAttributes(NodeAttribute.PROMPT_STYLE, NodeAttribute.FORM_CELL_STYLE, NodeAttribute.CELL_STYLE));
            }
            else if (!lColumnItem.isFiller()) {
              // If it's a cell for a field
              lCellClasses.addAll(SingleWidgetBuildHelper.getClassesForNodeFeatures(lColumnItem.getItemNode()));

              // Add styles just for the field cell
              lCellClasses.addAll(lColumnItem.getItemNode().getStringAttributes(NodeAttribute.FIELD_CELL_CLASS, NodeAttribute.FORM_TABLE_CLASS, NodeAttribute.FORM_CELL_CLASS, NodeAttribute.CELL_CLASS));
              lCellStyles.addAll(lColumnItem.getItemNode().getStringAttributes(NodeAttribute.FIELD_CELL_STYLE, NodeAttribute.FORM_TABLE_STYLE, NodeAttribute.FORM_CELL_STYLE, NodeAttribute.CELL_STYLE));
            }

            if (lCellClasses.size() > 0) {
              pSerialiser.append(" " + Joiner.on(" ").join(lCellClasses));
            }
            pSerialiser.append("\""); //End class attr

            if (lCellStyles.size() > 0) {
              pSerialiser.append(" style=\"" + Joiner.on(" ").join(lCellStyles) + "\"");
            }

            if(!lColumnItem.isFiller()) {
              HTMLSerialiser.serialiseDataAttributes(pSerialiser, lColumnItem.getItemNode());
            }

            pSerialiser.append(">");

            if (pSerialisationContext.getDevToolbarContext().isFlagOn(DevToolbarContext.Flag.HTML_GEN_DEBUG)) {
              SingleWidgetBuildHelper.outputDebugInformation(pSerialiser, lColumnItem, lFormColumns);
            }

            if (!lColumnItem.isFiller()) {
              if (lColumnItem.isPrompt()) {
                lColumnItem.getWidgetBuilder().buildPrompt(pSerialisationContext, pSerialiser, lColumnItem.getItemNode());
              }
              else {
                if (lColumnItem.getItemNode().hasHint() && lColumnItem.getItemNode().getWidgetBuilderType() != WidgetBuilderType.BUTTON) {
                  pSerialiser.append("<div class=\"eleven-column-input\">");
                }

                if(lColumnItem.getItemNode().hasDescription() && LayoutDirection.NORTH == lColumnItem.getItemNode().getDescriptionLayout()) {
                  pSerialiser.addDescription(pSerialisationContext, lColumnItem.getItemNode());
                }

                lColumnItem.getWidgetBuilder().buildWidget(pSerialisationContext, pSerialiser, lColumnItem.getItemNode());

                if(lColumnItem.getItemNode().hasDescription() && LayoutDirection.SOUTH == lColumnItem.getItemNode().getDescriptionLayout()) {
                  pSerialiser.addDescription(pSerialisationContext, lColumnItem.getItemNode());
                }

                SingleWidgetBuildHelper.insertErrorMessage(pSerialiser, lColumnItem.getItemNode());
                SingleWidgetBuildHelper.insertHistoryMessage(pSerialiser, lColumnItem.getItemNode());

                if (lColumnItem.getItemNode().hasHint() && lColumnItem.getItemNode().getWidgetBuilderType() != WidgetBuilderType.BUTTON) {
                  pSerialiser.append("</div>");
                  pSerialiser.append("<div class=\"input-group-addon hint-addon\">");
                  pSerialiser.addHint(pSerialisationContext, lColumnItem.getItemNode().getHint());
                  pSerialiser.append("</div>");
                }
              }
            }

            pSerialiser.append("</div>"); // Close the column div
          }
        }

        pSerialiser.append("</div>"); // Close the container div
      }
    }
    finally {
      Track.pop("FormWidget");
    }
  }
}
