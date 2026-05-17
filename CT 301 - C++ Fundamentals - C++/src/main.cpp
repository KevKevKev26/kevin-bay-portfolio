#include "GameOfLife.h"
#include "FileParser.h"
#include <fstream>
#include <iostream>
#include <sstream>
#include <stdexcept>
#include <string>
#include <vector>
#include <cstdlib>
#ifndef _WIN32
#include <unistd.h>
#endif

int main(int argc, char* argv[]) {

    std::string filename;
    int generations = 10;
    int print_interval = 1;
    bool three_state = false;
    bool wraparound = false;

    int pause_interval = -1;
    char live_char = '1';
    char dead_char = '0';
    char decay_char = 'D';
    std::string output_filename;
    int buffer_size = 100;
    int opt;

    //I couldnt get getopt to work on windows so I made one without it -Kevin
#ifndef _WIN32
    while ((opt = getopt(argc, argv, "f:g:p:swx:a:d:e:o:r:")) != -1) {
        switch (opt) {
            case 'f':
                filename = optarg;
                break;
            case 'g':
                try {
                    generations = std::stoi(optarg);
                    if (generations < 0) { std::cerr << "Error: Generations must be a non-negative number." << std::endl; return 1; }
                } catch (...) { std::cerr << "Error: Invalid generations value. Expected an integer." << std::endl; return 1; }
                break;
            case 'p':
                try {
                    print_interval = std::stoi(optarg);
                    if (print_interval <= 0) { std::cerr << "Error: Print interval must be a positive number." << std::endl; return 1; }
                } catch (...) { std::cerr << "Error: Invalid print interval value. Expected an integer." << std::endl; return 1; }
                break;
            case 's':
                three_state = true;
                break;
            case 'w':
                wraparound = true;
                break;
            case 'x':
                try {
                    pause_interval = std::stoi(optarg);
                    if (pause_interval <= 0) { std::cerr << "Error: Pause interval must be a positive number." << std::endl; return 1; }
                } catch (...) { std::cerr << "Error: Invalid pause interval value. Expected an integer." << std::endl; return 1; }
                break;
            case 'a':
                if (std::string(optarg).length() != 1) { std::cerr << "Error: -a requires a single character." << std::endl; return 1; }
                live_char = optarg[0];
                break;
            case 'd':
                if (std::string(optarg).length() != 1) { std::cerr << "Error: -d requires a single character." << std::endl; return 1; }
                dead_char = optarg[0];
                break;
            case 'e':
                if (std::string(optarg).length() != 1) { std::cerr << "Error: -e requires a single character." << std::endl; return 1; }
                decay_char = optarg[0];
                break;
            case 'o':
                output_filename = optarg;
                break;
            case 'r':
                try {
                    buffer_size = std::stoi(optarg);
                    if (buffer_size <= 0) { std::cerr << "Error: Buffer size must be a positive number." << std::endl; return 1; }
                } catch (...) { std::cerr << "Error: Invalid buffer size value. Expected an integer." << std::endl; return 1; }
                break;
            default:
                std::cerr << "Unknown argument or missing value." << std::endl;
                std::cerr << "Usage: GOLApp -f [filename] [-g generations] [-p print_interval] [-s] [-w] [-x pause] [-a live] [-d dead] [-e decay] [-o outfile] [-r buffer]" << std::endl;
                return 1;
        }
    }
#else
    for (int i = 1; i < argc; ++i) {
        std::string arg = argv[i];
        if (arg == "-f") {
            if (i + 1 >= argc) { std::cerr << "Error: -f requires a filename." << std::endl; return 1; }
            filename = argv[++i];
        } else if (arg == "-g") {
            if (i + 1 >= argc) { std::cerr << "Error: -g requires a number." << std::endl; return 1; }
            try { generations = std::stoi(argv[++i]); if (generations < 0) { std::cerr << "Error: Generations must be a non-negative number." << std::endl; return 1; } }
            catch (...) { std::cerr << "Error: Invalid generations value. Expected an integer." << std::endl; return 1; }
        } else if (arg == "-p") {
            if (i + 1 >= argc) { std::cerr << "Error: -p requires a number." << std::endl; return 1; }
            try { print_interval = std::stoi(argv[++i]); if (print_interval <= 0) { std::cerr << "Error: Print interval must be a positive number." << std::endl; return 1; } }
            catch (...) { std::cerr << "Error: Invalid print interval value. Expected an integer." << std::endl; return 1; }
        } else if (arg == "-s") {
            three_state = true;
        } else if (arg == "-w") {
            wraparound = true;
        } else if (arg == "-x") {
            if (i + 1 >= argc) { std::cerr << "Error: -x requires a number." << std::endl; return 1; }
            try { pause_interval = std::stoi(argv[++i]); if (pause_interval <= 0) { std::cerr << "Error: Pause interval must be a positive number." << std::endl; return 1; } }
            catch (...) { std::cerr << "Error: Invalid pause interval value. Expected an integer." << std::endl; return 1; }
        } else if (arg == "-a") {
            if (i + 1 >= argc || std::string(argv[i+1]).length() != 1) { std::cerr << "Error: -a requires a single character." << std::endl; return 1; }
            live_char = argv[++i][0];
        } else if (arg == "-d") {
            if (i + 1 >= argc || std::string(argv[i+1]).length() != 1) { std::cerr << "Error: -d requires a single character." << std::endl; return 1; }
            dead_char = argv[++i][0];
        } else if (arg == "-e") {
            if (i + 1 >= argc || std::string(argv[i+1]).length() != 1) { std::cerr << "Error: -e requires a single character." << std::endl; return 1; }
            decay_char = argv[++i][0];
        } else if (arg == "-o") {
            if (i + 1 >= argc) { std::cerr << "Error: -o requires a filename." << std::endl; return 1; }
            output_filename = argv[++i];
        } else if (arg == "-r") {
            if (i + 1 >= argc) { std::cerr << "Error: -r requires a number." << std::endl; return 1; }
            try { buffer_size = std::stoi(argv[++i]); if (buffer_size <= 0) { std::cerr << "Error: Buffer size must be a positive number." << std::endl; return 1; } }
            catch (...) { std::cerr << "Error: Invalid buffer size value. Expected an integer." << std::endl; return 1; }
        } else {
            std::cerr << "Unknown argument: " << arg << std::endl;
            std::cerr << "Usage: GOLApp -f [filename] [-g generations] [-p print_interval] [-s] [-w] [-x pause] [-a live] [-d dead] [-e decay] [-o outfile] [-r buffer]" << std::endl;
            return 1;
        }
    }
#endif

    if (live_char == dead_char || live_char == decay_char || dead_char == decay_char) {
        std::cerr << "Error: Cell characters for live, dead, and decay must all be unique." << std::endl;
        return 1;
    }

    if (filename.empty()) {
        std::cerr << "Error: No filename provided. Use -f [filename]." << std::endl;
        return 1;
    }

    if (three_state && wraparound) {
        std::cerr << "Error: -s and -w flags are mutually exclusive." << std::endl;
        return 1;
    }

    GameBoardData data;
    int parse_result = parseInputFile(filename, data, live_char, dead_char);
    if (parse_result != 0) {
        return parse_result;
    }

    GameOfLife game(data.width, data.height, data.game_string, three_state, wraparound, buffer_size);

    std::ofstream fout;
    if (!output_filename.empty()) {
        size_t last_slash_idx = output_filename.find_last_of("/");
        if (std::string::npos != last_slash_idx) {
            std::string dir_path = output_filename.substr(0, last_slash_idx);
            if (!dir_path.empty()) {
                std::string command = "mkdir -p " + dir_path;
                int system_result = system(command.c_str());
                if (system_result != 0) {
                    std::cerr << "Error: Failed to create directory '" << dir_path << "'. mkdir -p returned " << system_result << std::endl;
                    return 1;
                }
            }
        }

        fout.open(output_filename);
        if (!fout) {
            std::cerr << "Error: Could not open output file '" << output_filename << "'." << std::endl;
            return 1;
        }
    }

    auto printBoard = [&](std::ostream& os) {
        os << "Generation: " << game.getGeneration() << std::endl;
        for (int row = 0; row < data.height; ++row) {
            for (int col = 0; col < data.width; ++col) {
                int val = game.getCell(row, col);
                if (three_state && val == 2) os << decay_char;
                else if (val == 1) os << live_char;
                else os << dead_char;
            }
            os << std::endl;
        }
        os << std::endl;
    };

    if (output_filename.empty()) printBoard(std::cout);
    else printBoard(fout);

    int current_generation = 0;
    while (current_generation < generations) {
        game.next();
        ++current_generation;
        bool on_print = (current_generation % print_interval == 0);
        bool on_pause = (pause_interval > 0 && current_generation % pause_interval == 0 && current_generation != generations);

        if (on_print) {
            if (output_filename.empty()) printBoard(std::cout);
            else printBoard(fout);
        }

        if (on_pause) {
            std::cout << "Game Paused: What would you like to do?" << std::endl;
            std::cout << "Current GameBoard:" << std::endl;
            printBoard(std::cout);
            std::string cmd;
            while (true) {
                std::cout << "$ ";
                std::getline(std::cin, cmd);
                if (cmd.empty()) continue;
                std::istringstream iss(cmd);
                char action;
                iss >> action;
                if (action == 'x') break;
                if (action == 'r') {
                    int rollback_count = -1;
                    if (!(iss >> rollback_count) || rollback_count <= 0) {
                        std::cout << "Invalid rollback count." << std::endl;
                        continue;
                    }
                    if (!game.rollback(rollback_count)) {
                        std::cout << "Rollback failed: too many generations requested or buffer too small." << std::endl;
                        continue;
                    }
                    std::cout << "Current GameBoard:" << std::endl;
                    printBoard(std::cout);
                    continue;
                }
                int x = -1, y = -1;
                if (!(iss >> x)) {
                    std::cout << "Invalid command." << std::endl;
                    continue;
                }
                if (!(iss >> y)) {
                    int idx = x;
                    int row = idx / data.width;
                    int col = idx % data.width;
                    if (row < 0 || row >= data.height || col < 0 || col >= data.width) {
                        std::cout << "Invalid coordinate." << std::endl;
                        continue;
                    }
                    if (action == 'a') game.setCell(row, col, 1);
                    else if (action == 'd') game.setCell(row, col, 0);
                    else if (action == 'e') game.setCell(row, col, 2);
                    else { std::cout << "Unknown action." << std::endl; continue; }
                } else {
                    if (x < 0 || x >= data.height || y < 0 || y >= data.width) {
                        std::cout << "Invalid coordinate." << std::endl;
                        continue;
                    }
                    if (action == 'a') game.setCell(x, y, 1);
                    else if (action == 'd') game.setCell(x, y, 0);
                    else if (action == 'e') game.setCell(x, y, 2);
                    else { std::cout << "Unknown action." << std::endl; continue; }
                }
                std::cout << "Current GameBoard:" << std::endl;
                printBoard(std::cout);
            }
        }
    }

    if (fout.is_open()) fout.close();
    return 0;
}