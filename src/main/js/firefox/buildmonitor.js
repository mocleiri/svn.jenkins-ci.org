org_hudsonci.BuildMonitor = Base.extend({
	constructor: function(preferences, ui, downloader, logger, localiser, notification) {
		this.preferences = preferences;
		this.ui = ui;
		this.downloader = downloader;
		this.logger = logger;
		this.localiser = localiser;
		this.notification = notification;
		this.feeds = null;
	},
	getFeeds: function() {
		return this.feeds;
	},
	prepare: function() {
		localiser.setBundle(document.getElementById('hudson-stringbundle'));
		logger.debug(localiser.getText('monitor.init'));
	},
	runAll: function() {
		this.feeds = preferences.getFeeds();
		this.ui.prepare(this.feeds);

		var size = this.preferences.getSize();
		var feedStatusType = this.preferences.getFeedStatusType();
		this.executorCallback = new org_hudsonci.DownloaderCallback(TYPE_EXECUTOR, new org_hudsonci.ExecutorFeedParser(size), new org_hudsonci.ExecutorFeedNotifier(this.notification, this.preferences), this.logger, this.localiser, this.ui);
		this.historicCallback = new org_hudsonci.DownloaderCallback(TYPE_HISTORIC, new org_hudsonci.HistoricFeedParser(size, feedStatusType), new org_hudsonci.HistoricFeedNotifier(this.notification, this.preferences), this.logger, this.localiser, this.ui);
		for (var i = 0; i < this.feeds.length; i++) {
			if (!this.feeds[i].isIgnored()) {
				this.run(i);
			}
		}
	},
	removeFeed: function(i) {
		this.preferences.removeFeed(this.feeds[i]);
	},
	run: function(i) {
		if (this.preferences.getExecutor()) {
			downloader.download(this.executorCallback, this.feeds[i].getExecutorUrl(), this.feeds[i], this.preferences.getNetworkUsername(), this.preferences.getNetworkPassword());
		}
		downloader.download(this.historicCallback, this.feeds[i].getUrl(), this.feeds[i], this.preferences.getNetworkUsername(), this.preferences.getNetworkPassword());
	}
});