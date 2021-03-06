/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.webui.popup;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.forum.ForumUtils;
import org.exoplatform.forum.common.webui.UIPopupAction;
import org.exoplatform.forum.common.webui.UIPopupContainer;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.JCRPageList;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.UserProfile;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.webui.UIForumContainer;
import org.exoplatform.forum.webui.UIForumDescription;
import org.exoplatform.forum.webui.UIForumPageIterator;
import org.exoplatform.forum.webui.UIForumPortlet;
import org.exoplatform.forum.webui.UITopicDetail;
import org.exoplatform.forum.webui.UITopicDetailContainer;
import org.exoplatform.forum.webui.UITopicPoll;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(
    template = "app:/templates/forum/webui/popup/UIPageListTopicByUser.gtmpl",
    events = {
        @EventConfig(listeners = UIPageListTopicByUser.OpenTopicActionListener.class ),
        @EventConfig(listeners = UIPageListTopicByUser.SetOrderByActionListener.class ),
        @EventConfig(listeners = UIPageListTopicByUser.DeleteTopicActionListener.class,confirm="UITopicDetail.confirm.DeleteThisTopic" )
    }
)
public class UIPageListTopicByUser extends UIContainer {
  public static final int PAGE_SIZE = 5;

  private ForumService forumService;

  private JCRPageList  pageList;

  private String       strOrderBy = ForumUtils.EMPTY_STR;

  private String       userName   = ForumUtils.EMPTY_STR;

  private boolean      isUseAjax  = true;

  private List<Topic>  topics     = new ArrayList<Topic>();

  private Log          log        = ExoLogger.getLogger(UIPageListTopicByUser.class);

  public UIPageListTopicByUser() throws Exception {
    forumService = (ForumService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ForumService.class);
    addChild(UIForumPageIterator.class, null, "PageListTopicByUser");
  }

  private UserProfile getUserProfile() {
    UIForumPortlet forumPortlet = this.getAncestorOfType(UIForumPortlet.class);
    isUseAjax = forumPortlet.isUseAjax();
    return forumPortlet.getUserProfile();
  }

  public boolean isUseAjax() {
    return isUseAjax;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  @SuppressWarnings("unchecked")
  protected List<Topic> getTopicsByUser() throws Exception {
    UIForumPageIterator forumPageIterator = this.getChild(UIForumPageIterator.class);
    try {
      boolean isMod = false;
      if (getUserProfile().getUserRole() == 0)
        isMod = true;
      pageList = forumService.getPageTopicByUser(this.userName, isMod, strOrderBy);
      if (pageList != null)
        pageList.setPageSize(PAGE_SIZE);
      forumPageIterator.initPage(pageList.getPageSize(), pageList.getCurrentPage(), 
                                 pageList.getAvailable(), pageList.getAvailablePage());
      topics = pageList.getPage(forumPageIterator.getPageSelected());
      forumPageIterator.setSelectPage(pageList.getCurrentPage());
    } catch (Exception e) {
      log.trace("\nThe topic(s) must exist: " + e.getMessage() + "\n" + e.getCause());
    }
    return topics;
  }

  public Topic getTopicById(String topicId) throws Exception {
    for (Topic topic : topics) {
      if (topic.getId().equals(topicId))
        return topic;
    }
    return (Topic) forumService.getObjectNameById(topicId, Utils.TOPIC);
  }

  protected String[] getStarNumber(Topic topic) throws Exception {
    double voteRating = topic.getVoteRating();
    return ForumUtils.getStarNumber(voteRating);
  }

  static public class DeleteTopicActionListener extends EventListener<UIPageListTopicByUser> {
    public void execute(Event<UIPageListTopicByUser> event) throws Exception {
      UIPageListTopicByUser uiForm = event.getSource();
      String topicId = event.getRequestContext().getRequestParameter(OBJECTID);
      Topic topic = uiForm.getTopicById(topicId);
      String[] path = topic.getPath().split(ForumUtils.SLASH);
      int i = path.length;
      String categoryId = path[i - 3];
      String forumId = path[i - 2];
      uiForm.forumService.removeTopic(categoryId, forumId, topicId);
      UIModeratorManagementForm parent = uiForm.getAncestorOfType(UIModeratorManagementForm.class);
      if(parent != null) {
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
      }else {
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm.getAncestorOfType(UIForumPortlet.class));
      }
    }
  }

  static public class OpenTopicActionListener extends EventListener<UIPageListTopicByUser> {
    public void execute(Event<UIPageListTopicByUser> event) throws Exception {
      UIPageListTopicByUser uiForm = event.getSource();
      String topicId = event.getRequestContext().getRequestParameter(OBJECTID);
      Topic topic = uiForm.getTopicById(topicId);
      topic = uiForm.forumService.getTopicUpdate(topic, false);
      UIForumPortlet forumPortlet = uiForm.getAncestorOfType(UIForumPortlet.class);
      if (topic == null) {
        event.getRequestContext().getUIApplication().addMessage(new ApplicationMessage("UIShowBookMarkForm.msg.link-not-found",
                                                                                       null,
                                                                                       ApplicationMessage.WARNING));
        return;
      }
      
      String[] id = topic.getPath().split(ForumUtils.SLASH);
      int i = id.length;
      String categoryId = id[i - 3];
      String forumId = id[i - 2];
      Forum forum = new Forum();
      boolean isRead = true;
      if (forumPortlet.getUserProfile().getUserRole() > 0) {
        try {
          Category category = uiForm.forumService.getCategory(categoryId);
          forum = uiForm.forumService.getForum(categoryId, forumId);
          isRead = forumPortlet.checkCanView(category, forum, topic);
        } catch (Exception e) {
          event.getRequestContext().getUIApplication().addMessage(new ApplicationMessage("UIShowBookMarkForm.msg.link-not-found",
                                                                                         null,
                                                                                         ApplicationMessage.WARNING));
          return;
        }
      }
      if(!isRead) {
        event.getRequestContext().getUIApplication().addMessage(new ApplicationMessage("UIForumPortlet.msg.do-not-permission",
                                                                                       new String[] {},
                                                                                       ApplicationMessage.WARNING));
          return;
      }

      if (((UIComponent) uiForm.getParent()).getId().equals("UIModeratorManagementForm")) {
        UIModeratorManagementForm parentComponent = uiForm.getParent();
        UIPopupContainer popupContainer = parentComponent.getAncestorOfType(UIPopupContainer.class);
        UIPopupAction popupAction = popupContainer.getChild(UIPopupAction.class);
        UIViewTopic viewTopic = popupAction.activate(UIViewTopic.class, 700);
        viewTopic.setTopic(topic);
        viewTopic.setActionForm(new String[] { "Close" });
        event.getRequestContext().addUIComponentToUpdateByAjax(popupAction);
      } else {
        forumPortlet.updateIsRendered(ForumUtils.FORUM);
        UIForumContainer uiForumContainer = forumPortlet.getChild(UIForumContainer.class);
        UITopicDetailContainer uiTopicDetailContainer = uiForumContainer.getChild(UITopicDetailContainer.class);
        uiForumContainer.setIsRenderChild(false);
        uiForumContainer.getChild(UIForumDescription.class).setForum(forum);
        UITopicDetail uiTopicDetail = uiTopicDetailContainer.getChild(UITopicDetail.class);
        uiTopicDetail.setUpdateForum(forum);
        uiTopicDetail.initInfoTopic(categoryId, forumId, topic, 0);
        uiTopicDetailContainer.getChild(UITopicPoll.class).updateFormPoll(categoryId, forumId, topic.getId());
        uiTopicDetail.setIdPostView("top");
        forumPortlet.cancelAction();
        event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
      }
    }
  }

  static public class SetOrderByActionListener extends EventListener<UIPageListTopicByUser> {
    public void execute(Event<UIPageListTopicByUser> event) throws Exception {
      UIPageListTopicByUser uiContainer = event.getSource();
      String path = event.getRequestContext().getRequestParameter(OBJECTID);
      uiContainer.strOrderBy = ForumUtils.getOrderBy(uiContainer.strOrderBy, path);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiContainer);
    }
  }
}
