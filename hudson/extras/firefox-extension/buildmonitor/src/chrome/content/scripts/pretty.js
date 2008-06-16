/*
 * JavaScript Pretty Date
 * Copyright (c) 2008 John Resig (jquery.com)
 * Licensed under the MIT license.
 */

/*
 * Takes an ISO time and returns a string representing how long ago the date represents.
 * Slightly modified from original code (http://ejohn.org/blog/javascript-pretty-date/),
 * to work with UTC instead of current locale time.
 */
function prettyDateUTC(time) {
	var date = new Date((time || "").replace(/-/g,"/").replace(/[TZ]/g," ")),
		diff = (((getCurrentDateUTC()).getTime() - date.getTime()) / 1000),
		day_diff = Math.floor(diff / 86400);

	if ( isNaN(day_diff) || day_diff < 0 || day_diff >= 31 )
		return;

	return day_diff == 0 && (
			diff < 60 && "just now" ||
			diff < 120 && "1 minute ago" ||
			diff < 3600 && Math.floor( diff / 60 ) + " minutes ago" ||
			diff < 7200 && "1 hour ago" ||
			diff < 86400 && Math.floor( diff / 3600 ) + " hours ago") ||
		day_diff == 1 && "Yesterday" ||
		day_diff < 7 && day_diff + " days ago" ||
		day_diff < 31 && Math.ceil( day_diff / 7 ) + " weeks ago";
}
function getCurrentDateUTC() {
	var date = new Date();
	var localTime = date.getTime();
	var localOffset = date.getTimezoneOffset() * 60000;
	return new Date(localTime + localOffset);
}