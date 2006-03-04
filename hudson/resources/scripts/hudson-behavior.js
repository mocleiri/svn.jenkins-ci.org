var hudsonRules = {
  ".advancedButton" : function(e) {
    e.onclick = function() {
      var link = this.parentNode;
      var container = link.nextSibling;
      container.style.display = "block";
      link.style.display = "none";
    }
  },

  ".pseudoLink" : function(e) {
    e.onmouseover = function() {
      this.style.textDecoration="underline";
    }
    e.onmouseout = function() {
      this.style.textDecoration="none";
    }
  }
};

Behaviour.start();
Behaviour.register(hudsonRules);