package main

import (
	"bufio"
	"crypto/md5"
	"flag"
	"fmt"
	"io"
	"os"
	"path/filepath"
	"sort"
	"strings"
    "strconv"
)

type Descending struct{}

func (d Descending) String() string {
	return "Descending"
}

func (d Descending) Sort(keys []int64) {
	sort.Slice(keys, func(i, j int) bool { return keys[i] > keys[j] })
}

type Ascending struct{}

func (a Ascending) String() string {
	return "Ascending"
}

func (a Ascending) Sort(keys []int64) {
	sort.Slice(keys, func(i, j int) bool { return keys[i] < keys[j] })
}

type FileSorter struct {
	FileType       string
	SortingOption  interface{}
	SortingOptions map[int]interface{}
}

func NewFileSorter() *FileSorter {
	sortingOptions := make(map[int]interface{})
	sortingOptions[1] = Descending{}
	sortingOptions[2] = Ascending{}

	return &FileSorter{
		SortingOptions: sortingOptions,
	}
}

func (f *FileSorter) SetSortingOption(option int) {
	f.SortingOption = f.SortingOptions[option]
}

func (f *FileSorter) WalkDirectory(path string) map[int64][]string {
	fileDict := make(map[int64][]string)

	filepath.Walk(path, func(filePath string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if !info.IsDir() {
			fileSize := info.Size()
			fileExtension := filepath.Ext(filePath)

			if f.FileType == "" || strings.TrimPrefix(fileExtension, ".") == f.FileType {
				fileDict[fileSize] = append(fileDict[fileSize], filePath)
			}
		}
		return nil
	})

	return fileDict
}

func main() {
	pathPtr := flag.String("path", "", "the parent directory")
	flag.Parse()

	if *pathPtr == "" {
		fmt.Println("Directory is not specified")
		return
	}

	fileSorter := NewFileSorter()
	fileSorter.FileType = "txt" // Assuming default file type is "txt"

	fmt.Println("Size sorting options:")
	fmt.Println("1. Descending")
	fmt.Println("2. Ascending")

	fmt.Print("Enter a sorting option: ")
	reader := bufio.NewReader(os.Stdin)
	optionStr, _ := reader.ReadString('\n')
	option := strings.TrimSpace(optionStr)

	if option != "1" && option != "2" {
		fmt.Println("Wrong option")
		return
	}

	sortingOption, _ := strconv.Atoi(option)
	fileSorter.SetSortingOption(sortingOption)

	fileDict := fileSorter.WalkDirectory(*pathPtr)
	keys := make([]int64, 0, len(fileDict))
	for k := range fileDict {
		keys = append(keys, k)
	}
	sort.Slice(keys, func(i, j int) bool { return keys[i] < keys[j] })

	for _, key := range keys {
		fmt.Printf("%d bytes\n", key)
		for _, file := range fileDict[key] {
			fmt.Println(file)
		}
		fmt.Println()
	}

	// Check for duplicates and delete files if needed
}

func md5Checksum(filePath string) (string, error) {
	file, err := os.Open(filePath)
	if err != nil {
		return "", err
	}
	defer file.Close()

	hash := md5.New()
	if _, err := io.Copy(hash, file); err != nil {
		return "", err
	}

	return fmt.Sprintf("%x", hash.Sum(nil)), nil
}
