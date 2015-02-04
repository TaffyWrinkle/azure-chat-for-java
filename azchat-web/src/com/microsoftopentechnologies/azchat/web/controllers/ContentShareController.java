/*
 Copyright 2015 Microsoft Open Technologies, Inc.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package com.microsoftopentechnologies.azchat.web.controllers;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.microsoftopentechnologies.azchat.web.common.exceptions.AzureChatException;
import com.microsoftopentechnologies.azchat.web.common.utils.AzureChatConstants;
import com.microsoftopentechnologies.azchat.web.common.utils.AzureChatUtils;
import com.microsoftopentechnologies.azchat.web.common.utils.ServiceActionEnum;
import com.microsoftopentechnologies.azchat.web.data.beans.MessageCommentBean;
import com.microsoftopentechnologies.azchat.web.data.beans.UserBean;
import com.microsoftopentechnologies.azchat.web.data.beans.UserMessageBean;
import com.microsoftopentechnologies.azchat.web.data.beans.UserMessageListBean;
import com.microsoftopentechnologies.azchat.web.services.BaseService;

/**
 * This controller handles the User Content share requests.User request for
 * upload text,photos and videos and also for the retrieval of the uploaded
 * content are handled.
 * 
 * @author Dnyaneshwar_Pawar
 *
 */
@Controller
public class ContentShareController extends BaseController {

	private static final Logger LOGGER = LogManager
			.getLogger(ContentShareController.class);

	@Autowired
	@Qualifier("contentShareService")
	private BaseService contentShareService;

	/**
	 * This method handles the user request for content share includes text
	 * message,photo.vedio.Also parse the response into JSON format.
	 * 
	 * @param request
	 * @return
	 * @throws AzureChatException
	 */
	@RequestMapping(value = AzureChatConstants.FROM_PAGE_CONTENT_SHARE, method = RequestMethod.POST)
	public @ResponseBody UserBean updateUserStatus(
			MultipartHttpServletRequest request, HttpServletResponse response)
			throws AzureChatException {
		LOGGER.info("[ContentShareController][updateUserStatus] start");
		UserBean userBean = new UserBean();
		populateUserBean(userBean, request);
		if (!userBean.hasErrors()) {
			userBean.setServiceAction(ServiceActionEnum.CONTENT_SHARE);
			userBean = (UserBean) contentShareService.invokeService(userBean);
			if (!userBean.hasErrors()) {
				userBean.setMultipartFile(null);	
				if (null != userBean.getUserMessageListBean()) {

					String medType = userBean.getUserMessageListBean()
							.getMediaType();
					if (null != medType && medType.contains(AzureChatConstants.UI_MEDIA_TYPE_VIDEO)) {
						userBean.setMsg(AzureChatConstants.SUCCESS_MSG_CONTENT_SHARE
								+ AzureChatConstants.CONSTANT_SPACE
								+ AzureChatConstants.SUCCESS_MSG_VIDEO_SHARE);
					} else {
						userBean.setMsg(AzureChatConstants.SUCCESS_MSG_CONTENT_SHARE);
					}
					userBean.setUserMessageListBean(null);
				}

			}
		}
		LOGGER.info("[ContentShareController][updateUserStatus] end");
		return userBean;

	}

	/**
	 * This method populates the user bean value from multi-part request
	 * 
	 * @param userBean
	 * @param request
	 * @throws AzureChatException
	 */
	private void populateUserBean(UserBean userBean,
			MultipartHttpServletRequest request) throws AzureChatException {
		UserMessageListBean userMessageListBean = new UserMessageListBean();
		MultipartFile photoVedioFile = request.getFile("mediaPhoto");
		if (null != photoVedioFile) {
			if (AzureChatUtils.getMegaBytes(photoVedioFile.getSize()) > AzureChatUtils
					.getNumbers(AzureChatUtils
							.getProperty(AzureChatConstants.MAX_UPLOAD_SIZE_KEY))) {
				AzureChatUtils
						.populateErrors(
								userBean,
								AzureChatConstants.EXCEP_CODE_BUSSINESS_EXCEPTION,
								AzureChatConstants.EXCEP_MSG_UPLOAD_SIZE_EXCEEDS
										+ AzureChatUtils
												.getProperty(AzureChatConstants.MAX_UPLOAD_SIZE_KEY));
				userBean.setHasErrors(true);

			} else {
				userMessageListBean.setPhotoVedioFile(photoVedioFile);
			}
			userMessageListBean.setMediaType(photoVedioFile.getContentType());
		}
		userMessageListBean.setExpiryTime(request.getParameter("expiryTime"));
		userMessageListBean.setMsgText(request.getParameter("msgText"));
		userBean.setUserMessageListBean(userMessageListBean);
		userBean.setUserID(request.getParameter("logedInUserID"));
		userBean.setNameID(request.getParameter("nameID"));
		userBean.setPhotoUrl(request.getParameter("photoUrl"));
	}

	/**
	 * This method handles user request to add the comment to the existing user
	 * message.Also parse the response into JSON format.
	 * 
	 * @return JSON MesageCommentBean
	 */
	@RequestMapping(value = AzureChatConstants.FROM_PAGE_UPDATE_MSG_COMMENT, method = RequestMethod.POST)
	public @ResponseBody MessageCommentBean updateMessageComment(
			HttpServletRequest request) {
		LOGGER.info("[ContentShareController][updateMessageComment] start");
		MessageCommentBean messageCommentBean = new MessageCommentBean();
		populateMessageCommentBean(messageCommentBean, request);
		messageCommentBean
				.setServiceAction(ServiceActionEnum.UPDATE_MSG_COMMENT);
		messageCommentBean = (MessageCommentBean) contentShareService
				.invokeService(messageCommentBean);
		if (!messageCommentBean.hasErrors()) {
			LOGGER.debug("Comment updated successfully.");
			messageCommentBean
					.setMsg(AzureChatConstants.SUCCESS_MSG_COMMENT_UPDATE);
		}
		LOGGER.info("[ContentShareController][updateMessageComment] end");
		return messageCommentBean;
	}

	/**
	 * This method handles the user message like status updates request.Also
	 * parse the response into JSON format.
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(value = AzureChatConstants.FROM_PAGE_USER_LIKE, method = RequestMethod.POST)
	public @ResponseBody UserMessageBean updateUserMessageLikeStatus(
			HttpServletRequest request) {
		LOGGER.info("[ContentShareController][updateUserMessageLikeStatus] start");
		UserMessageBean userMessageBean = new UserMessageBean();
		populateUserMessageBean(userMessageBean, request);
		userMessageBean
				.setServiceAction(ServiceActionEnum.UPDATE_USER_LIKE_STATUS);
		userMessageBean = (UserMessageBean) contentShareService
				.invokeService(userMessageBean);
		if (!userMessageBean.hasErrors()) {
			userMessageBean.setMsg(AzureChatConstants.SUCCESS_MSG_LIKE_STATUS);
		}
		LOGGER.info("[ContentShareController][updateUserMessageLikeStatus] end");
		return userMessageBean;
	}

	/**
	 * This method handles the user content retrieval requests and parse the
	 * response into JSON format.
	 * 
	 * @param request
	 * @return userBean
	 */
	@RequestMapping(value = AzureChatConstants.FROM_PAGE_GET_USER_CONTENT, method = RequestMethod.POST)
	public @ResponseBody UserBean getUserContents(HttpServletRequest request) {
		LOGGER.info("[ContentShareController][getUserContents] start");
		UserBean userBean = new UserBean();
		userBean.setUserID(request.getParameter("logedInUserID"));
		userBean.setContentLevel(request.getParameter("contentType"));
		userBean.setServiceAction(ServiceActionEnum.GET_USER_CONTENT);
		userBean = (UserBean) contentShareService.invokeService(userBean);
		if (!userBean.hasErrors()) {
			LOGGER.debug("User details fetched successfully.");
		}
		LOGGER.info("[ContentShareController][getUserContents] end");
		return userBean;
	}

	/**
	 * This method handles the request for get user message comment for the
	 * single message.Also parse the response into the JSON format.
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping(value = AzureChatConstants.FROM_PAGE_GET_MSG_COMMENTS, method = RequestMethod.POST)
	public @ResponseBody UserMessageBean getMessageComments(
			HttpServletRequest request) {
		LOGGER.info("[ContentShareController][getMessageComments] start");
		UserMessageBean userMessageBean = new UserMessageBean();
		userMessageBean.setMsgID(request.getParameter("msgID"));
		userMessageBean.setServiceAction(ServiceActionEnum.GET_MSG_COMMENTS);
		userMessageBean = (UserMessageBean) contentShareService
				.invokeService(userMessageBean);
		if (!userMessageBean.hasErrors()) {
			LOGGER.debug("User comments fetched successfuly");
		}
		LOGGER.info("[ContentShareController][getMessageComments] end");
		return userMessageBean;
	}

	/**
	 * This method populates the request parameter values into the
	 * UserMessageBean.
	 * 
	 * @param userMessageBean
	 * @param request
	 */
	private void populateUserMessageBean(UserMessageBean userMessageBean,
			HttpServletRequest request) {
		String operation = request.getParameter("operation");
		userMessageBean.setOwnerID(request.getParameter("logedInUserID"));
		userMessageBean.setOwnerName(request.getParameter("userName"));
		userMessageBean.setPhotoUrl(request.getParameter("photoUrl"));
		userMessageBean.setMsgID(request.getParameter("msgID"));
		if ("like".equalsIgnoreCase(operation)) {
			userMessageBean.setIsLike(true);
		} else {
			userMessageBean.setIsLike(false);
		}
	}

	/**
	 * This method populates message comment bean details from the ajax request.
	 * 
	 * @param messageCommentBean
	 * @param request
	 */
	private void populateMessageCommentBean(
			MessageCommentBean messageCommentBean, HttpServletRequest request) {
		messageCommentBean.setComment(request.getParameter("comment"));
		messageCommentBean.setFriendID(request.getParameter("logedInUserID"));
		messageCommentBean.setFriendName(request.getParameter("userName"));
		messageCommentBean.setMsgID(request.getParameter("msgID"));
		messageCommentBean.setPhotoUrl(request.getParameter("photoUrl"));
	}
}