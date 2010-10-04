/*******************************************************************************
 * Copyright (c) 2006-2007 Koji Hisano <hisano@gmail.com> - UBION Inc. Developer
 * Copyright (c) 2006-2007 UBION Inc. <http://www.ubion.co.jp/>
 * 
 * Copyright (c) 2006-2007 Skype Technologies S.A. <http://www.skype.com/>
 * 
 * Skype4Java is licensed under either the Apache License, Version 2.0 or
 * the Eclipse Public License v1.0.
 * You may use it freely in commercial and non-commercial products.
 * You may obtain a copy of the licenses at
 *
 *   the Apache License - http://www.apache.org/licenses/LICENSE-2.0
 *   the Eclipse Public License - http://www.eclipse.org/legal/epl-v10.html
 *
 * If it is possible to cooperate with the publicity of Skype4Java, please add
 * links to the Skype4Java web site <https://developer.skype.com/wiki/Java_API> 
 * in your web site or documents.
 * 
 * Contributors:
 * Koji Hisano - initial API and implementation
 * Bart Lamot - good javadocs
 ******************************************************************************/
package com.skype;

/**
 * Implementation of the ChatMessageListener.
 * Overide the methods to use for event trigger.
 * @see ChatMessageListener
 * @author Koji Hisano
 */
public class ChatMessageAdapter implements ChatMessageListener {
	/**
	 * This method is called when a chatmessage is received.
	 * @param receivedChatMessage the actual message.
	 * @throws SkypeException when the connection has gone bad.
	 */
	public void chatMessageReceived(ChatMessage receivedChatMessage) throws SkypeException {
    }

	/**
	 * This method is called when a chatmessage us sent.
	 * @param sentChatMessage the chatmessage that has been sent.
	 * @throws SkypeException when the connection has gone bad.
	 */
    public void chatMessageSent(ChatMessage sentChatMessage) throws SkypeException {
    }
}
