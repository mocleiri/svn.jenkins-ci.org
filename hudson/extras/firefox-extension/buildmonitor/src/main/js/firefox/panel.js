org_hudsonci.Panel = org_hudsonci.UiElement.extend({
	prepare: function(container, feed, hideName) {
		var panel = document.createElement('statusbarpanel');
		panel.setAttribute('id', this._getPanelId(feed));
		if (this.isExecutor()) {
			panel.setAttribute('class', 'statusbarpanel-iconic');
		} else if (this.isHistoric()) {
			panel.setAttribute('class', 'statusbarpanel-iconic-text');
			if (!hideName) {
				panel.setAttribute('label', feed.getName());
			}
		}
		panel.setAttribute('popup', 'buildmonitor-builds');
		panel.setAttribute('context', 'buildmonitor-menu');
		panel.setAttribute('tooltip', 'buildmonitor-tooltip');
		container.appendChild(panel);
	},
	set: function(status, feed) {
		this.getPanelElement(feed).setAttribute('src', 'chrome://buildmonitor/skin/' + status + '.png');
	},
	getPanelElement: function(feed) {
		return document.getElementById(this._getPanelId(feed));
	},
	_getPanelId: function(feed) {
		return 'hudson-panel-' + this.getUiElementId(feed);
	}
});