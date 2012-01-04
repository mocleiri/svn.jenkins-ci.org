require 'spec_helper'
require "calculator"

describe Calculator, "#add" do
  it "returns 2 for adding 1+1" do
    Calculator.new.add(1,1).should == 2
  end
end

describe Calculator, "#sub" do
  it "returns 0 for subtracting 1-1" do
    Calculator.new.sub(1,1).should == 0
  end
end
