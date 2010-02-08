org_hudsonci.MenusPopup = org_hudsonci.UiElement.extend({
	constructor: function(type, localiser) {
		this.base(type, localiser);
	},
	prepare: function(container, feed) {
		var menusMenupopup = document.createElement('menupopup');
		menusMenupopup.setAttribute('id', this._getMenusMenupopupId(feed));
		container.appendChild(menusMenupopup);
	},
	set: function(container, feed) {
		var menupopup = this._getMenusMenupopupElement(feed);
	
		var preferencesMenuItem = document.createElement('menuitem');
		preferencesMenuItem.setAttribute('label', this.localiser.getText('menu.preferences'));
		preferencesMenuItem.setAttribute('oncommand', 'org_hudsonci_openPreferencesWindow();');
		preferencesMenuItem.setAttribute('class', 'menuitem-iconic');
		preferencesMenuItem.setAttribute('image', 'chrome://buildmonitor/skin/menu/preferences.png');
		menupopup.appendChild(preferencesMenuItem);
		
		var refreshAllMenuitem = document.createElement('menuitem');
		refreshAllMenuitem.setAttribute('label', this.localiser.getText('menu.refresh.all'));
		refreshAllMenuitem.setAttribute('oncommand', 'org_hudsonci.runAll();');
		refreshAllMenuitem.setAttribute('class', 'menuitem-iconic');
		refreshAllMenuitem.setAttribute('image', 'chrome://buildmonitor/skin/menu/refreshall.png');
		menupopup.appendChild(refreshAllMenuitem);
		
		menupopup.appendChild(document.createElement('menuseparator'));
	
		var removeMenuItem = document.createElement('menuitem');
		removeMenuItem.setAttribute('label', this.localiser.getText('menu.remove') + ' [' + feed.getName() + ']');
		removeMenuItem.setAttribute('oncommand', 'org_hudsonci.removeFeed(' + feed.getId() + ');');
		removeMenuItem.setAttribute('class', 'menuitem-iconic');
		removeMenuItem.setAttribute('image', 'chrome://buildmonitor/skin/menu/remove.png');
		menupopup.appendChild(removeMenuItem);
		
		var dashboardMenuItem = document.createElement('menuitem');
		dashboardMenuItem.setAttribute('label', this.localiser.getText('menu.dashboard') + ' [' + feed.getName() + ']');
		dashboardMenuItem.setAttribute('oncommand', 'org_hudsonci.goToDashboard(' + feed.getId() + ');');
		dashboardMenuItem.setAttribute('class', 'menuitem-iconic');
		dashboardMenuItem.setAttribute('image', 'chrome://buildmonitor/skin/menu/dashboard.png');
		menupopup.appendChild(dashboardMenuItem);
		
		if (feed.isJob()) {
			var buildMenuItem = document.createElement('menuitem');
			buildMenuItem.setAttribute('label', this.localiser.getText('menu.build') + ' [' + feed.getName() + ']');
			buildMenuItem.setAttribute('oncommand', 'org_hudsonci.build(' + feed.getId() + ');');
			buildMenuItem.setAttribute('class', 'menuitem-iconic');
			buildMenuItem.setAttribute('image', 'chrome://buildmonitor/skin/menu/build.png');
			menupopup.appendChild(buildMenuItem);
		}
				
		var refreshMenuitem = document.createElement('menuitem');
		refreshMenuitem.setAttribute('label', this.localiser.getText('menu.refresh') + ' [' + feed.getName() + ']');
		refreshMenuitem.setAttribute('oncommand', 'org_hudsonci.run(' + feed.getId() + ');');
		refreshMenuitem.setAttribute('class', 'menuitem-iconic');
		refreshMenuitem.setAttribute('image', 'chrome://buildmonitor/skin/menu/refresh.png');
		menupopup.appendChild(refreshMenuitem);
		   	
		container.setAttribute('context', this._getMenusMenupopupId(feed));
	},
	_getMenusMenupopupElement: function(feed) {
		return document.getElementById(this._getMenusMenupopupId(feed));
	},
	_getMenusMenupopupId: function(feed) {
		return 'hudson-menupopup-menus-' + this.getUiElementId(feed);
	}
});