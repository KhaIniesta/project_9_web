package hcmute.controller.user;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import hcmute.entity.MilkTeaEntity;
import hcmute.service.IMilkTeaCategoryService;
import hcmute.service.IMilkTeaService;
import hcmute.service.IMilkTeaTypeService;

@Controller
@RequestMapping("header")
public class HeaderController {
	@Autowired
	IMilkTeaCategoryService milkTeaCategoryService;
	@Autowired
	IMilkTeaTypeService milkTeaTypeService;
	@Autowired
	IMilkTeaService milkTeaService;

	@RequestMapping("search/content={name}")
	public String getMilkTeaByNameContaining(@PathVariable("name") String encodedName, Model model,
			@RequestParam("page") Optional<Integer> page) {
		String name;
        name = URLDecoder.decode(encodedName, StandardCharsets.UTF_8);
        model.addAttribute("content", name);
        int count = milkTeaService.countByNameContaining(name);
        int currentPage = page.orElse(1);
        int pageSize = 8;

        Pageable pageable = PageRequest.of(currentPage - 1, pageSize, Sort.by("id_milk_tea"));
        Page<MilkTeaEntity> resultpaPage = null;
        resultpaPage = milkTeaService.findByNameContaining(name, pageable);
        int totalPages = resultpaPage.getTotalPages();
        if (totalPages > 0) {
            int start = Math.max(1, currentPage - 2);
            int end = Math.min(currentPage + 2, totalPages);
            if (totalPages > count) {
                if (end == totalPages)
                    start = end - count;
                else if (start == 1)
                    end = start + count;
            }
            List<Integer> pageNumbers = IntStream.rangeClosed(start, end).boxed().collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }
        model.addAttribute("milkTeasByNames", resultpaPage);

        return "user/search";
	}

	@RequestMapping("search/content={name}/method={method}")
	public String searchAndSort(@PathVariable("name") String encodedName, @PathVariable("method") String method,
			Model model, @RequestParam("page") Optional<Integer> page) {
		try {
			String name = URLDecoder.decode(encodedName, StandardCharsets.UTF_8.toString());
			model.addAttribute("content", name);

			int count = milkTeaService.countByNameContaining(name);
			int currentPage = page.orElse(1);
			int pageSize = 8;

			Pageable pageable = PageRequest.of(currentPage - 1, pageSize, Sort.by("id_milk_tea"));
			Page<MilkTeaEntity> resultPage = milkTeaService.findByNameContaining(name, pageable);
			int totalPages = resultPage.getTotalPages();
			if (totalPages > 0) {
				int start = Math.max(1, currentPage - 2);
				int end = Math.min(currentPage + 2, totalPages);
				if (totalPages > count) {
					if (end == totalPages)
						start = end - count;
					else if (start == 1)
						end = start + count;
				}
				List<Integer> pageNumbers = IntStream.rangeClosed(start, end).boxed().collect(Collectors.toList());
				model.addAttribute("pageNumbers", pageNumbers);
			}
			model.addAttribute("milkTeaBySorts", resultPage);

			if ("outstanding".equals(method)) {
				List<MilkTeaEntity> milkTeas = milkTeaService.findByNameContaining(name);
				milkTeaService.sortByOrderDetailQuantity(milkTeas);
				// Chuyển đổi List<MilkTeaEntity> sang Page<MilkTeaEntity>
				// Tính toán index bắt đầu và kết thúc của trang
				int start = (int) pageable.getOffset();
				int end = Math.min((start + pageable.getPageSize()), milkTeas.size());
				// Tạo một sublist của danh sách đã sắp xếp để giữ lại các phần tử của trang cụ thể
				List<MilkTeaEntity> pagedMilkTeas = milkTeas.subList(start, end);
				// Tạo một đối tượng PageImpl mới với danh sách đã cắt và thông tin phân trang từ resultPage
				Page<MilkTeaEntity> milkTeaPage = new PageImpl<>(pagedMilkTeas, pageable, milkTeas.size());
				model.addAttribute("milkTeaBySorts", milkTeaPage);
				
			} else if ("low-to-high".equals(method)) {
				Pageable sortPageable = PageRequest.of(currentPage - 1, pageSize, Sort.by("cost").ascending());
				Page<MilkTeaEntity> milkTeas = milkTeaService.findByNameContaining(name, sortPageable);
				model.addAttribute("milkTeaBySorts", milkTeas);
				
			} else if ("high-to-low".equals(method)) {
				Pageable sortPageable = PageRequest.of(currentPage - 1, pageSize, Sort.by("cost").descending());
				Page<MilkTeaEntity> milkTeas = milkTeaService.findByNameContaining(name, sortPageable);
				model.addAttribute("milkTeaBySorts", milkTeas);
			}

			return "user/search";
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "error";
		}
	}

	@GetMapping("/moveToSearchPage")
	public RedirectView moveToSearchPage(RedirectAttributes redirectAttributes,
			@RequestParam("content") String content) {
        String encodedContent = URLEncoder.encode(content, StandardCharsets.UTF_8);
        return new RedirectView("/header/search/content=" + encodedContent);
    }

}
