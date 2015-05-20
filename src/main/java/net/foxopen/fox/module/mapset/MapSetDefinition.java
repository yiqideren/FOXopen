/*

Copyright (c) 2010, UK DEPARTMENT OF ENERGY AND CLIMATE CHANGE -
                    ENERGY DEVELOPMENT UNIT (IT UNIT)
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.
    * Neither the name of the DEPARTMENT OF ENERGY AND CLIMATE CHANGE nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

$Id$

*/
package net.foxopen.fox.module.mapset;


import net.foxopen.fox.ContextLabel;
import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.module.Validatable;
import net.foxopen.fox.command.XDoCommandList;
import net.foxopen.fox.command.XDoIsolatedRunner;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.dom.PathOrDOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExCardinality;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.storage.CacheKey;
import net.foxopen.fox.thread.storage.StorageLocationBind;
import net.foxopen.fox.thread.storage.UsingType;
import net.foxopen.fox.track.Track;


public abstract class MapSetDefinition
implements Validatable {
  //If mapset timeout is longer than 7 days assume it is effectively static. Legacy developers tended to put a very large value like 999999999.
  private static long STATIC_MAPSET_TIMEOUT_THRESHOLD_MINS = 7 * 24 * 60;

  /** Mapset name within its module */
  public final String mLocalName;
  /** Engine-level Mapset name including module name prefix */
  public final String mFullName;

  /** App which owns this MapSetDefinition's module. */
  public final String mAppMnem;

  private final CacheKey mCacheKey;
  /** True if the mapset contains a UNIQUE cache key bind and is thus scoped to a module call. */
  private final boolean mScopedToModuleCall;

  /** Parsed contents of fm:do block. May be null. */
  private final XDoCommandList mXDoCommandList;

  public final long mRefreshTimeoutMins;

  protected MapSetDefinition(String pLocalName, CacheKey pCacheKey, XDoCommandList pXDo, long pRefreshTimeoutMins, Mod pModule) {
    mLocalName = pLocalName;
    mFullName = pModule.getName() + "/" + pLocalName;
    mCacheKey = pCacheKey;

    //Validate that a cache key has been either explicitly specified or generated by a sub-class
    if(pCacheKey == null) {
      throw new ExInternal("Cache key not defined and no auto-generated cache key available for mapset " + pLocalName);
    }
    Track.debug("MapSetCacheKey", mCacheKey);

    mXDoCommandList = pXDo;
    mRefreshTimeoutMins = pRefreshTimeoutMins;
    mAppMnem = pModule.getApp().getMnemonicName();


    //Establish if the definition is scoped to a module call (i.e. it has a UNIQUE bind)
    boolean lScopedToModuleCall = false;
    for(StorageLocationBind lBind : mCacheKey.getUsingBinds()) {
      if(lBind.getUsingType() == UsingType.UNIQUE) {
        lScopedToModuleCall = true;
        break;
      }
    }
    mScopedToModuleCall = lScopedToModuleCall;
  }

  protected final DOM createDefaultContainerDOM() {
    return DOM.createDocument(MapSet.MAPSET_LIST_ELEMENT_NAME);
  }

  /**
   * Creates a new MapSet for this definition.
   * @param pRequestContext Current RequestContext.
   * @param pItemDOM The element the MapSet is being constructed for. This may be null depending on the MapSet specificity
   * (i.e. if it is dependent on :{itemrec}).
   * @param pEvaluatedCacheKey The cache key that has been evaluated for this mapset.
   * @param pUniqueValue The value which would have been used for the UNIQUE cache key bind.
   * @return A new MapSet.
   */
  protected MapSet createMapSet(ActionRequestContext pRequestContext, DOM pItemDOM, String pEvaluatedCacheKey, String pUniqueValue) {

    DOM lMapSetContainerDOM = createMapSetDOM(pRequestContext, pItemDOM, pEvaluatedCacheKey, pUniqueValue);

    runXDoCommands(pRequestContext, lMapSetContainerDOM);

    return DOMMapSet.createFromDOM(lMapSetContainerDOM, this, pEvaluatedCacheKey);
  }

  /**
   * Constructs a new mapset DOM according to the definition. This should be in the format /map-set-list/map-set/rec etc.
   * @param pRequestContext Current RequestContext.
   * @param pItemDOM The element the MapSet is being constructed for. This may be null depending on the MapSet specificity
   * (i.e. if it is dependent on :{itemrec}).
   * @param pEvaluatedCacheKey The cache key that has been evaluated for this mapset.
   * @param pUniqueValue The value which would have been used for the UNIQUE cache key bind.
   * @return A new DOM containing the evaluated mapset.
   */
  protected abstract DOM createMapSetDOM(ActionRequestContext pRequestContext, DOM pItemDOM, String pEvaluatedCacheKey, String pUniqueValue);

  /**
   * Provides a key which instantiated MapSets can be stored against for subsequent resolution (i.e. not via their cache key).
   * This is valuable for reset mapset functionality, where a mapset name is targeted and many MapSet instances will
   * need to be resolved. Subclasses may override the behaviour of this method to provide their instantiated MapSets with
   * a definition key which gives them an appropriate level of granularity.
   * @return The key for this definition.
   */
  String getDefinitionKey() {
    return mFullName;
  }

  /**
   * Sets the :{item} and :{itemrec} contexts if an item DOM is available. The :{map-set-attach} context is set to either
   * the path specified or to :{itemrec} if no path is given.
   *
   * This should only be called on a ContextUElem where you have called {@link ContextUElem#localise(String)} and should {@link ContextUElem#delocalise(String)} after use.
   *
   * @param pContextUElem localised context to apply labels to
   * @param pItemDOM
   * @param pMapSetAttach
   */
  public void setupContextUElem(ContextUElem pContextUElem, DOM pItemDOM, PathOrDOM pMapSetAttach) {

    if(pItemDOM != null) {
      pContextUElem.setUElem(ContextLabel.ITEM, pItemDOM);
      pContextUElem.setUElem(ContextLabel.ITEMREC, pItemDOM.getParentOrSelf());
    }

    if(pMapSetAttach.isPath()) {
      String lMapSetAttachXPath = pMapSetAttach.getPath();
      try {
        pContextUElem.setUElem(ContextLabel.MAP_SET_ATTACH, pContextUElem.extendedXPath1E(pItemDOM.getParentOrSelf(), lMapSetAttachXPath, false));
      }
      catch (ExCardinality ex) {
        throw new ExInternal("Failed to set :{map-set-attach} using XPath '" + lMapSetAttachXPath + "'", ex);
      }
      catch (ExActionFailed ex) {
        throw new ExInternal("Failed to set :{map-set-attach} using XPath '" + lMapSetAttachXPath + "'", ex);
      }
    }
    else if(pMapSetAttach.isDOM()) {
      pContextUElem.setUElem(ContextLabel.MAP_SET_ATTACH, pMapSetAttach.getDOM());
    }
    else {
      // Default to itemrec equivalent
      if (pItemDOM != null) {
        pContextUElem.setUElem(ContextLabel.MAP_SET_ATTACH, pItemDOM.getParentOrSelf());
      }
    }
  }

  /**
   * Gets a MapSet from cache or creates one if the cached copy is not available or requires refreshing. This method will
   * setup the RequestContext's ContextUElem with the required mapset item/itemrec contexts, evaluate the cache key for
   * this MapSetDefinition, and perform a cache lookup. If this MapSet definition is scoped to the module call or is marked
   * as "refresh always", the local cache from the MapSetManager will be used, otherwise the global MapSet cache will be used.
   * The MapSet will be created if it is not cached, or recreated and replaced into the "top" of the LRU cache if a refresh
   * was required.<br/><br/>
   *
   * The MapSetManager is used to track which MapSets have been refreshed in a processing cycle (i.e. a page churn). If
   * the target MapSet has already been refreshed in a churn it will not be refreshed again even if it is marked as "refresh always".
   *
   * @param pRequestContext Current RequestContext.
   * @param pItemDOM DOM element the MapSet lookup is being performed for. This can be null, although the MapSet definition
   * should contain no reference to :{item} or :{itemrec} contexts if it is null - an error will occur if it does.
   * All entry points to this method should  provide the module developer with the ability to specify the MapSet's item DOM.
   * @param pMapSetAttach An additional path or DOM for setting the :{map-set-attach} context to. The object should be provided
   * but is not required to contain either argument type.
   * @param pUniqueConstant String for UNIQUE bind evaluation - typically the module call ID.
   * @param pMapSetManager For local cache lookup and refresh tracking.
   * @return The target MapSet.
   */
  public final MapSet getOrCreateMapSet(ActionRequestContext pRequestContext, DOM pItemDOM, PathOrDOM pMapSetAttach, String pUniqueConstant, MapSetManager pMapSetManager) {

    //TODO properly deal with itemDOM being null for all cases

    ContextUElem lContextUElem = pRequestContext.getContextUElem();

    Track.pushInfo("getMapSet");
    try {
      lContextUElem.localise("getOrCreateMapSet " + mFullName);
      try {
        setupContextUElem(lContextUElem, pItemDOM, pMapSetAttach);

        String lEvaluatedCacheKey = evaluateCacheKey(lContextUElem, pUniqueConstant);
        Track.debug("MapSetCacheKey", lEvaluatedCacheKey);

        MapSet lCachedMapSet;
        if(isCachedLocally()) {
          lCachedMapSet = pMapSetManager.getLocalCachedMapSet(lEvaluatedCacheKey);
        }
        else {
          lCachedMapSet = MapSet.getFromCache(lEvaluatedCacheKey);
        }

        if(lCachedMapSet == null || (!pMapSetManager.isProcessed(lEvaluatedCacheKey) && lCachedMapSet.isRefreshRequired())) {
          //If no mapset is in cache, or a refresh is required and we haven't already refreshed the mapset in this churn, recreate the mapset
          Track.debug("MapSetRefresh", lCachedMapSet == null ? "MapSet not in cache" : "MapSet refresh required");

          //Delegate to subclass the new mapset
          MapSet lNewMapSet = createMapSet(pRequestContext, pItemDOM, lEvaluatedCacheKey, pUniqueConstant);

          //Store the new mapset in the correct cache based on its scope
          if(isCachedLocally()) {
            pMapSetManager.addLocalCachedMapSet(lNewMapSet);
          }
          else {
            MapSet.addToCache(lNewMapSet);
          }

          //Record that the MapSet has been processed so it is not re-processed for this request
          pMapSetManager.markMapSetProcessed(lEvaluatedCacheKey);

          return lNewMapSet;
        }
        else {
          Track.debug("MapSetCacheHit");
          return lCachedMapSet;
        }
      }
      catch (Throwable th) {
        throw new ExInternal("Error getting mapset " + mFullName, th);
      }
      finally {
        lContextUElem.delocalise("getOrCreateMapSet " + mFullName);
      }
    }
    finally {
      Track.pop("getMapSet");
    }
  }

  private String evaluateCacheKey(ContextUElem pContextUElem, String pUniqueConstant) {
    String lCacheKeyPrefix = mAppMnem + "/" + mFullName;
    String lEvaluatedCacheKey = mCacheKey.evaluate(lCacheKeyPrefix, pContextUElem, pUniqueConstant);
    return lEvaluatedCacheKey;
  }

  /**
   * Tests if this mapset definition represents a dynamic mapset, i.e. if it may change between page churns.
   * @return True if dynamic.
   */
  final boolean isDynamic() {
    return mRefreshTimeoutMins < STATIC_MAPSET_TIMEOUT_THRESHOLD_MINS;
  }

  final long getRefreshTimeoutMins() {
    return mRefreshTimeoutMins;
  }

  /**
   * Tests if this MapSet is appropriate for global caching or should be cached locally. Mapsets which always refresh
   * or are scoped to a module call should be cached locally.
   * @return True if MapSet should be cached locally due to its limited scope.
   */
  final boolean isCachedLocally() {
    return isScopedToModuleCall() || isRefreshAlways();
  }

  final boolean isScopedToModuleCall() {
    return mScopedToModuleCall;
  }

  final boolean isRefreshAlways() {
    return mRefreshTimeoutMins == 0;
  }

  protected final XDoCommandList getXDoCommandList() {
    return mXDoCommandList;
  }

  protected final void runXDoCommands(ActionRequestContext pRequestContext, DOM pMapSetDOM) {

    XDoCommandList lCommandList = getXDoCommandList();
    if(lCommandList != null) {
      ContextUElem lContextUElem = pRequestContext.getContextUElem();
      lContextUElem.localise("Map-Set do");
      try {
        lContextUElem.setUElem(ContextLabel.ATTACH, pMapSetDOM);
        XDoIsolatedRunner lCommandRunner = pRequestContext.createIsolatedCommandRunner(true);
        lCommandRunner.runCommandsAndComplete(pRequestContext, lCommandList);
      }
      finally {
        lContextUElem.delocalise("Map-Set do");
      }
    }
  }

   /**
   * Validates that the map-set, and its sub-components, are valid.
   *
   * @param module the module where the component resides
   * @throws ExInternal if the component syntax is invalid.
   */
  public final void validate(Mod module) {
    if (mXDoCommandList != null) {
      mXDoCommandList.validate(module);
    }
  }

  public final String getLocalName() {
    return mLocalName;
  }

  public final String getFullName() {
    return mFullName;
  }
}
