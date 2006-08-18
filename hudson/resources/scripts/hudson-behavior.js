var hudsonRules = {
  ".advancedButton" : function(e) {
    e.onclick = function() {
      var link = this.parentNode;
      link.style.display = "none"; // hide the button

      var container = link.nextSibling.firstChild; // TABLE -> TBODY

      var tr = link;
      while(tr.tagName!="TR")
        tr = tr.parentNode;

      // move the contents of the advanced portion into the main table
      while(container.lastChild!=null) {
        tr.parentNode.insertBefore(container.lastChild,tr.nextSibling);
      }
    }
  },

  ".pseudoLink" : function(e) {
    e.onmouseover = function() {
      this.style.textDecoration="underline";
    }
    e.onmouseout = function() {
      this.style.textDecoration="none";
    }
  },

  ".help-button" : function(e) {
    e.onclick = function() {
      // identify the parent TR
      var tr = this;
      while(tr.tagName!="TR")
        tr = tr.parentNode;

      // then next TR
      do {
        tr = tr.nextSibling;
      } while(tr.tagName!="TR");

      div = tr.firstChild.nextSibling.firstChild;

      if(div.style.display!="block") {
        div.style.display="block";
        // make it visible
        new Ajax.Request(
            this.getAttribute("helpURL"),
            {
              method : 'get',
              onComplete : function(x) {
                div.innerHTML = x.responseText;
              }
            }
        );
      } else {
        div.style.display = "none";
      }

      return false;
    }
  }
};

Behaviour.start();
Behaviour.register(hudsonRules);