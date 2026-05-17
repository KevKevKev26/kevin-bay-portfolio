#ifndef GAMEOFLIFE_H
#define GAMEOFLIFE_H

#include <string>
#include <vector>

class GameOfLife {
public:
    GameOfLife(int width, int height, const std::string& game_string, bool three_state = false, bool wraparound = false, int buffer_size = 100);

    void next();
    void nextNGen(int n);
    void printGame() const;

    int getCell(int row, int col) const;
    void setCell(int row, int col, int value);
    int getGeneration() const;

    void saveState();
    bool rollback(int count);
    int getBufferSize() const;
    int getHistoryCount() const;

private:
    int countLiveNeighbors(int row, int col) const;

    int width_;
    int height_;
    int generation_;
    std::vector<int> board_;
    bool three_state_;
    bool wraparound_;

    int buffer_size_;
    std::vector<std::vector<int>> history_;
    std::vector<int> generation_numbers_;
};

#endif