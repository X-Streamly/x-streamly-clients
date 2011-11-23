Gem::Specification.new do |s|
  s.name        = 'xstreamly'
  s.version     = '0.6.4'
  s.date        = '2011-11-01'
  s.summary     = "X-Stream.ly Client"
  s.description = "Send X-Stream.ly messages and manage callbacks"
  s.authors     = ["Brian Willard"]
  s.email       = 'bwillard@x-stream.ly'
  s.homepage    = 'http://x-stream.ly'
  s.files       = ["lib/xstreamly.rb"]
  s.add_runtime_dependency "json",["~>1.6"]
end
