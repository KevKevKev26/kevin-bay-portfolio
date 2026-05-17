CXX = g++
CXXFLAGS = -Wall -g -std=c++11

SRCS = main.cpp GameOfLife.cpp FileParser.cpp
OBJS = $(SRCS:.cpp=.o)
TARGET = GOLApp

all: $(TARGET)

$(TARGET): $(OBJS)
	$(CXX) $(CXXFLAGS) -o $(TARGET) $(OBJS)

clean:
	rm -f $(OBJS) $(TARGET)