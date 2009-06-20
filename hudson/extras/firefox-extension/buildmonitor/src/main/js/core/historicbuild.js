var HudsonHistoricBuild = HudsonBuild.extend ({
	init: function(name, url, date) {
		this._super(name, url);
		this.date = date;
	},
	getDate: function() {
		return this.date;
	},
	getStatus: function() {
		return new String(this.name.match('[(][A-Za-z]+[)]')).replace(/[(]/, '').replace(/[)]/, '').toLowerCase();
	},
	getDetails: function() {
		return this.name + ' - ' + prettyDateUTC(this.date);
	}
});