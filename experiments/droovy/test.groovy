def pid() {
  return new File("/proc/self").canonicalPath
}

println "connecting"
remote = connect("server1"); 

i=0
(0..<4).each {
  i++;
  println "trying ${i}"
  remote {
    println "1st from ${pid()} (${i})"
  }
  println "3rd from ${pid()} (${i})"
}
