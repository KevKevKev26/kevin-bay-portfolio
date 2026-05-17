#include "FileParser.h"
#include <iostream>
#include <fstream>
#include <sstream>
#include <limits>


bool validateFirstLine(const std::string& line, int& height, int& width) {
    std::stringstream ss(line);
    if (ss >> height && ss >> width && ss.eof()) {
        if (width <= 0 || height <= 0) {
            std::cerr << "Error: Board dimensions must be positive." << std::endl;
            return false;
        }
        return true;
    } else {
        std::cerr << "Error on line 1: Incorrect formatting (expected 'height width')." << std::endl;
        return false;
    }
}

int parseInputFile(const std::string& filename, GameBoardData& data, char live_char, char dead_char) {
    std::ifstream inFile(filename);
    if (!inFile) {
        std::cerr << "File Error: Cannot open file '" << filename << "'." << std::endl;
        return 1;
    }

    std::string firstLine;
    if (std::getline(inFile, firstLine)) {
        size_t endpos = firstLine.find_last_not_of(" \t\n\r");
        if (std::string::npos != endpos) {
            firstLine = firstLine.substr(0, endpos + 1);
        }

        if (!validateFirstLine(firstLine, data.height, data.width)) {
            return 2;
        }
    } else {
        std::cerr << "File Error: File is empty." << std::endl;
        return 1;
    }

    std::string current_line;
    int lineNumber = 2;
    int actualRows = 0;
    std::string temp_game_string;
    temp_game_string.reserve(data.width * data.height);

    while (std::getline(inFile, current_line)) {
        if (current_line.find_first_not_of(" \t\n\r") == std::string::npos) {
            continue;
        }
        actualRows++;

        std::string row_string;
        if(current_line.length() != static_cast<size_t>(data.width)){
            std::cerr << "Formatting Error on line " << lineNumber << ": Invalid number of columns. Expected " << data.width << ", got " << current_line.length() << "." << std::endl;
            return lineNumber + 1;
        }

        for(char c : current_line){
            if (c != live_char && c != dead_char) {
                std::cerr << "Invalid character on line " << lineNumber << ": Expected '" << live_char << "' or '" << dead_char << "'." << std::endl;
                return lineNumber + 1;
            }
            row_string += (c == live_char ? '1' : '0');
        }
        
        temp_game_string += row_string;
        lineNumber++;
    }

    if (data.height <= 0 || data.width <= 0 || actualRows == 0) {
        std::cerr << "Error: Board dimensions must be positive." << std::endl;
        return 2;
    }

    if (actualRows != data.height) {
        std::cerr << "Formatting Error: Invalid number of rows. Expected " << data.height << ", got " << actualRows << "." << std::endl;
        return lineNumber + 1;
    }

    data.game_string = temp_game_string;
    inFile.close();
    return 0;
}
