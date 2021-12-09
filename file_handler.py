import argparse
from collections import defaultdict
import hashlib
import os


def parse_args():
    parser = argparse.ArgumentParser(description='List contents of directory.')
    parser.add_argument('path', nargs='?', type=str, help='the parent directory', default=None)
    return parser.parse_args()


def valid_args(argparse_args):
    if argparse_args.path and os.path.exists(argparse_args.path):
        return True
    else:
        return False


def md5(file_name):
    hash_md5 = hashlib.md5()
    with open(file_name, "rb") as f:
        for chunk in iter(lambda: f.read(4096), b""):
            hash_md5.update(chunk)
    return hash_md5.hexdigest()


class Descending:

    def __str__(self):
        return "Descending"

    def __init__(self):
        self.reverse = True

    def sort(self, dictionary) -> list:
        return sorted(dictionary.keys(), reverse=self.reverse)


class Ascending:

    def __str__(self):
        return "Ascending"

    def __init__(self):
        self.reverse = False

    def sort(self, dictionary: defaultdict) -> list:
        return sorted(dictionary.keys(), reverse=self.reverse)


class FileSorter:

    sorting_options = {
        1: Descending(),
        2: Ascending()
    }

    def set_sorting_option(self, sorting_option):
        if sorting_option in self.sorting_options.keys():
            return self.sorting_options[sorting_option]
        return None

    def __init__(self):
        self.file_type = input("Enter file format:\n")

        print("Size sorting options:")
        for i, sorting_option in self.sorting_options.items():
            print(f"{i}. {sorting_option}")

        self.sorting_option = None
        while self.sorting_option is None:
            self.sorting_option = self.set_sorting_option(int(input("Enter a sorting option:\n")))
            if self.sorting_option is None:
                print("Wrong option")

    def os_walk(self, start_directory):
        file_dict = defaultdict(list)
        for root, dirs, files in os.walk(start_directory, topdown=True):
            for name in files:
                file_name = os.path.join(root, name)
                file_size = os.stat(file_name).st_size
                _, file_extension = os.path.splitext(file_name)

                if self.file_type == "" or self.file_type == file_extension[1:]:
                    file_dict[file_size].append(file_name)

        return Files(file_dict)


class Files:

    def check_duplicates(self):
        hash_dict = dict()
        for size, files in self.file_dict.items():
            hash_list = defaultdict(list)
            for file_name in files:
                hash_list[md5(file_name)].append(file_name)
            hash_dict[size] = hash_list
        return hash_dict

    def __init__(self, file_dict):
        self.file_dict = file_dict
        self.hash_dict = self.check_duplicates()
        self.id_duplicate = defaultdict(dict)

    def print_duplicates(self, sizes):
        i = 1
        for size in sizes:
            print(f"{size} bytes")
            for md5_hash, files in self.hash_dict[size].items():
                if len(files) == 1:
                    continue
                print(f"Hash: {md5_hash}")
                for file in files:
                    self.id_duplicate[i] = {"file": file, "size": size}
                    print(f"{i}. {file}")
                    i += 1
            print()

    def delete(self, ids: list[int]):
        freed = 0
        for i in ids:
            os.remove(self.id_duplicate[i]["file"])
            freed += self.id_duplicate[i]["size"]
        return freed


def yes_no_question(question):
    while True:
        user_input = input(f"{question}\n")
        if user_input == 'yes':
            return True
        elif user_input == 'no':
            return False
        else:
            print("Wrong option")


def nums_list(question):
    while True:
        file_nums_str = input(f"{question}\n").strip().split()
        try:
            file_nums = [int(n) for n in file_nums_str]
            if len(file_nums) > 0:
                return file_nums
            else:
                print("Wrong format")
        except ValueError:
            print("Wrong format")


def main():
    args = parse_args()
    if valid_args(args):
        file_sorter = FileSorter()

        my_files = file_sorter.os_walk(args.path)
        sort_type = file_sorter.sorting_option

        sorted_keys = sort_type.sort(my_files.file_dict)
        for key in sorted_keys:
            print(f"{key} bytes")
            for file in my_files.file_dict[key]:
                print(file)
            print()

        if yes_no_question("Check for duplicates?"):
            my_files.print_duplicates(sorted_keys)

        if yes_no_question("Delete files?"):
            file_nums = nums_list("Enter file numbers to delete:")
            freed = my_files.delete(file_nums)
            print(f"Total freed up space: {freed} bytes")

    else:
        print("Directory is not specified")


if __name__ == '__main__':
    main()
