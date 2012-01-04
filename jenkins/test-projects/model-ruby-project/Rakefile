require 'rspec/core/rake_task'
require 'ci/reporter/rake/rspec'

ENV['CI_REPORTS'] = Dir.pwd+"/reports"

desc "Run tests"
RSpec::Core::RakeTask.new(:spec => ["ci:setup:rspec"]) do |t|
  t.pattern = 'spec/**/*_spec.rb'
end

task :default => :spec
