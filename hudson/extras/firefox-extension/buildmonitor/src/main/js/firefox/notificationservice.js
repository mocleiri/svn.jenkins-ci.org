var HudsonNotificationService = Base.extend ({
	constructor: function(ffAlertsService, ffSoundService, ffIoService) {
		this.ffAlertsService = ffAlertsService;
		this.ffSoundService = ffSoundService;
		this.ffIoService = ffIoService;
		this.uiUtil = new HudsonUiUtil();
	},
	displayAlert: function(type, title, feed, build) {
		this.ffAlertsService.showAlertNotification('chrome://buildmonitor/skin/' + this.uiUtil.getStatusSkinType(type) + '/' + feed.getStatus() + '.png', title + ' [' + feed.getName() + ']', build.getDetails(), false);
	},
	playSound: function(feed) {
		this.ffSoundService.play(this.ffIoService.newURI('chrome://buildmonitor/skin/audio/' + feed.getStatus() + '.wav', null, null));
	}
});