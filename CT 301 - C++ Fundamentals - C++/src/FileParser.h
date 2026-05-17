#ifndef FILEPARSER_H
#define FILEPARSER_H

#include <string>
#include <vector>

struct GameBoardData {
    int width = 0;
    int height = 0;
    std::string game_string;
};


int parseInputFile(const std::string& filename, GameBoardData& data, char live_char, char dead_char);

#endif
