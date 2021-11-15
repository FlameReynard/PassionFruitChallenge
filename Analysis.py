import os


def analyse():
    stats_file = open("Stats.txt", "w+")
    header = "File name,Average accuracy,Average Best Epoch,Average error,Best accuracy,Average Epoch,Best error"
    print(header)
    for file_name in os.listdir("Results"):
        file = open("Results/" + file_name)
        lines = file.readlines()
        value_map = {}
        max_map = {}
        for line in lines:
            index = line.find(":")
            if index < 0:
                continue
            key = line[:index]
            value = line[index+2:]
            value = float(value[:value.find("\n")])

            if key not in value_map:
                value_map.update({key: value})
                max_map.update({key: value})
            else:
                old_value = value_map.get(key)
                value_map.update({key: value + old_value})
                if old_value < value:
                    max_map.update({key: value})

        count = 10
        stats_file.write(file_name + "\n")
        stats_file.write("===============================\n")
        csv = file_name
        for key in value_map:
            value = value_map.get(key)
            stat = value/count
            stats_file.write(key + "Average:%f" % stat + "\n")
            if key.__contains__("Best accuracy"):
                csv += ",%f" % stat
            elif key.__contains__("Best accuracy error"):
                csv += ",%f" % stat
        stats_file.write("-------------------------------\n")
        for key in max_map:
            value = max_map.get(key)
            stats_file.write(key + "Max:%f" % value + "\n")
            if key.__contains__("Best accuracy"):
                csv += ",%f" % value
            elif key.__contains__("Best accuracy error"):
                csv += ",%f" % value
        stats_file.write("===============================\n")
        print(csv)
