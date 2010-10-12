def pid() {
  return new File("/proc/self").canonicalPath
}

println "connecting"
remote = connect(); 

i=0
(0..<4).each {
  i++;
  println "Looping ${i}/4"
  remote {
    println "hello from ${pid()} (${i})"
  }
  println "hello from ${pid()} (${i})"
}
