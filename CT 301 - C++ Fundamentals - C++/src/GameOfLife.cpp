#include "GameOfLife.h"
#include <iostream>
#include <vector>


GameOfLife::GameOfLife(int width, int height, const std::string& game_string, bool three_state, bool wraparound, int buffer_size)
    : width_(width), height_(height), generation_(0), three_state_(three_state), wraparound_(wraparound), buffer_size_(buffer_size) {
    board_.reserve(width * height);
    for (char c : game_string) {
        board_.push_back(c - '0');
    }
    saveState();
}


void GameOfLife::next() {
    std::vector<int> next_board(width_ * height_);
    for (int row = 0; row < height_; ++row) {
        for (int col = 0; col < width_; ++col) {
            int live_neighbors = countLiveNeighbors(row, col);
            int current_cell_index = row * width_ + col;
            int current_cell_state = board_[current_cell_index];
            if (!three_state_) {
                if (current_cell_state == 1) {
                    if (live_neighbors == 2 || live_neighbors == 3) {
                        next_board[current_cell_index] = 1;
                    } else {
                        next_board[current_cell_index] = 0;
                    }
                } else {
                    if (live_neighbors == 3) {
                        next_board[current_cell_index] = 1;
                    } else {
                        next_board[current_cell_index] = 0;
                    }
                }
            } else {
                if (current_cell_state == 1) {
                    if (live_neighbors == 2 || live_neighbors == 3) {
                        next_board[current_cell_index] = 1;
                    } else {
                        next_board[current_cell_index] = 2;
                    }
                } else if (current_cell_state == 0) {
                    if (live_neighbors == 3) {
                        next_board[current_cell_index] = 2;
                    } else {
                        next_board[current_cell_index] = 0;
                    }
                } else {
                    if (live_neighbors == 3) {
                        next_board[current_cell_index] = 1;
                    } else if (live_neighbors == 2) {
                        next_board[current_cell_index] = 2;
                    } else {
                        next_board[current_cell_index] = 0;
                    }
                }
            }
        }
    }
    board_ = next_board;
    generation_++;
    saveState();
}
void GameOfLife::saveState() {
    if ((int)history_.size() == buffer_size_) {
        history_.erase(history_.begin());
        generation_numbers_.erase(generation_numbers_.begin());
    }
    history_.push_back(board_);
    generation_numbers_.push_back(generation_);
}

bool GameOfLife::rollback(int count) {
    if (count <= 0 || count >= (int)history_.size()) return false;
    int idx = history_.size() - 1 - count;
    board_ = history_[idx];
    generation_ = generation_numbers_[idx];
    history_.erase(history_.begin() + idx + 1, history_.end());
    generation_numbers_.erase(generation_numbers_.begin() + idx + 1, generation_numbers_.end());
    return true;
}

int GameOfLife::getBufferSize() const { return buffer_size_; }
int GameOfLife::getHistoryCount() const { return (int)history_.size(); }

void GameOfLife::nextNGen(int n) {
    if (n <= 0) {
        return;
    }
    for (int i = 0; i < n; ++i) {
        next();
    }
}

void GameOfLife::printGame() const {
    std::cout << "Generation: " << generation_ << std::endl;
    for (int row = 0; row < height_; ++row) {
        for (int col = 0; col < width_; ++col) {
            int val = board_[(row * width_) + col];
            if (three_state_ && val == 2) {
                std::cout << 'D';
            } else {
                std::cout << val;
            }
        }
        std::cout << std::endl;
    }
    std::cout << std::endl;
}

int GameOfLife::countLiveNeighbors(int row, int col) const {
    int live_neighbors = 0;
    for (int r_offset = -1; r_offset <= 1; ++r_offset) {
        for (int c_offset = -1; c_offset <= 1; ++c_offset) {
            if (r_offset == 0 && c_offset == 0) {
                continue; 
            }

            int neighbor_row = row + r_offset;
            int neighbor_col = col + c_offset;

            if (wraparound_) {
                if (neighbor_row < 0) neighbor_row = (neighbor_row + height_) % height_;
                else if (neighbor_row >= height_) neighbor_row = neighbor_row % height_;

                if (neighbor_col < 0) neighbor_col = (neighbor_col + width_) % width_;
                else if (neighbor_col >= width_) neighbor_col = neighbor_col % width_;
            }

            if (neighbor_row >= 0 && neighbor_row < height_ &&
                neighbor_col >= 0 && neighbor_col < width_) {
                int neighbor_index = (neighbor_row * width_) + neighbor_col;
                if (board_[neighbor_index] == 1) {
                    live_neighbors++;
                }
            }
        }
    }
    return live_neighbors;
}

int GameOfLife::getCell(int row, int col) const {
    if (row < 0 || row >= height_ || col < 0 || col >= width_) return 0;
    return board_[row * width_ + col];
}

void GameOfLife::setCell(int row, int col, int value) {
    if (row < 0 || row >= height_ || col < 0 || col >= width_) return;
    board_[row * width_ + col] = value;
}

int GameOfLife::getGeneration() const {
    return generation_;
}